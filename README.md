# Smarthack

Platformă hackathon pentru transformarea materialelor de curs (PDF/DOC/PPT etc.) în rezumate și întrebări grilă, cu autentificare pe roluri și integrare locală cu LLM-uri rulate prin Ollama.

Repository-ul conține două componente principale:

- dodo: backend Spring Boot (REST API) cu Postgres, JWT și roluri (ADMIN, PROFESOR, STUDENT)
- ai-service: microserviciu FastAPI care apelează Ollama pentru generare de rezumate, quiz-uri și hint-uri

## Ce face aplicația

- profesor/admin încarcă un fișier (material de curs)
- backend-ul extrage textul din fișier cu Apache Tika
- backend-ul trimite textul către ai-service
- ai-service generează:
  - rezumat concis (din text, fără informații externe)
  - set de întrebări grilă cu 4 opțiuni și indexul răspunsului corect
- backend-ul persistă întrebările în Postgres, asociate unui domeniu (domain)
- student poate:
  - cere hint pentru o întrebare (endpoint dedicat)
  - lista întrebărilor pe domeniu
  - verifica un răspuns pentru o întrebare

## Arhitectură

- Spring Boot 3 (Java 21)
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - Flyway pentru migrații
  - PostgreSQL driver
  - Apache Tika pentru extracție de text
  - JJWT pentru token-uri JWT
- FastAPI (Python 3.11)
  - endpoint pentru hint
  - endpoint pentru generare rezumat + quiz-uri
  - requests către Ollama OpenAI-compatible endpoint (/v1/chat/completions)
  - sentence-transformers (model multilingual MiniLM) încărcat pentru utilități de procesare (nu este folosit intens în fluxul curent)

Flux de date (simplificat):

- upload material -> extract text (Tika) -> ai-service (/generate-summary-quizzes) -> întrebări salvate în Postgres
- hint -> backend (/api/ai/hint) -> ai-service (/hint) -> răspuns scurt

## Servicii și porturi

- backend Spring Boot: 8080
- Postgres: 6666 (mapped la 5432 în container)
- ai-service (FastAPI): 8001
- Ollama: 11434

## Rulare locală cu Docker

Există două fișiere docker compose:

- docker-compose.yml (în root): Postgres + Ollama + ai-service
- dodo/docker.compose: Postgres + aplicația Spring Boot

Variantă A: rulezi totul (backend + DB) din dodo/docker.compose și ai-service separat

- pornește ai-service + Ollama (din root):
```bash
docker compose up --build
```

- pornește backend-ul (din folderul dodo):
```bash
docker compose -f docker.compose up --build
```

Observații:
- compose-ul din root pornește și Postgres pe portul 6666; compose-ul din dodo pornește și el Postgres pe portul 6666.
  - rulează doar unul dintre ele, altfel ai conflict de port
- backend-ul are default:
  - spring.datasource.url=jdbc:postgresql://localhost:6666/mydb
  - ai.service.url=http://localhost:8001

Variantă B: rulezi backend-ul din IDE și doar infrastructura din root

- pornește Postgres + Ollama + ai-service (din root):
```bash
docker compose up --build
```

- pornește Spring Boot din IntelliJ sau cu Maven (din folderul dodo):
```bash
./mvnw spring-boot:run
```

## Configurare

Backend (dodo/src/main/resources/application.properties):

- server.port=8080
- spring.datasource.url=jdbc:postgresql://localhost:6666/mydb
- spring.datasource.username=dodo
- spring.datasource.password=mydodopassword
- ai.service.url=http://localhost:8001
- spring.servlet.multipart.max-file-size=50MB
- spring.servlet.multipart.max-request-size=50MB

ai-service (docker-compose.yml din root):

- OLLAMA_BASE_URL=http://ollama:11434
- PRIMARY_MODEL=llama3.1:8b-instruct-q4_K_M
- SECONDARY_MODEL=qwen2.5:7b-instruct-q4_K_M

Compose-ul face pull automat pentru modelele:
- qwen2.5:7b-instruct-q4_K_M
- llama3.1:8b-instruct-q4_K_M

## Securitate și roluri

- autentificare pe bază de username + password
- token JWT emis la login
- endpoint-urile sunt protejate cu @PreAuthorize, în funcție de rol:
  - PROFESOR/ADMIN: upload materiale
  - PROFESOR: adăugare întrebări manual
  - STUDENT: hint, check answer, consum întrebări (unele sunt lăsate intenționat fără protecție în cod)

Rolurile sunt:
- ADMIN
- PROFESOR
- STUDENT

## API (backend)

Prefix: /api

Autentificare

- POST /api/auth/register
  - body JSON: { "username": "...", "password": "...", "role": "STUDENT|PROFESOR|ADMIN" }
  - creează user + setează rol

- POST /api/auth/login
  - body JSON: { "username": "...", "password": "..." }
  - răspuns: token JWT și detalii user (în funcție de implementarea curentă)

AI / hint

- POST /api/ai/hint
  - rol: STUDENT
  - body JSON: { "question": "..." , "context": "..." (opțional) }
  - proxy către ai-service /hint
  - răspuns: { "answer": "..." }

Materiale (upload + generare)

- POST /api/materials/upload
  - rol: PROFESOR sau ADMIN
  - multipart/form-data:
    - file: fișierul de încărcat
    - domain: string (default general)
  - răspuns:
    - filename
    - domain
    - summary
    - saved_questions
    - quizzes (question, options, answerIndex)

Întrebări

- POST /api/questions/add
  - rol: PROFESOR
  - body: QuestionDto
  - persistă o întrebare manual

- GET /api/questions/getQuestionsByDomain/{domain}
  - răspuns: listă de QuestionDto

- GET /api/questions/checkAnswer
  - rol: STUDENT
  - body: { "questionId": <id>, "answer": "..." }
  - întoarce true/false
  - observație: endpoint-ul este definit ca GET cu body, ceea ce nu este standard HTTP; pentru producție ar trebui mutat pe POST

## API (ai-service)

- POST /hint
  - body: { "question": "...", "context": "..." (opțional) }
  - răspuns: { "answer": "..." }
  - răspuns scurt, fără pașii interni, fără explicații lungi

- POST /generate-summary-quizzes
  - body: { "text": "...", "max_questions": <int> (opțional) }
  - răspuns JSON:
    - summary: string
    - quizzes: listă de itemi:
      - question: string
      - options: [a,b,c,d]
      - answer_index: 0..3
      - hint: string scurt

## Persistență și migrații

- Postgres
- Flyway migration scripts în dodo/src/main/resources/db/migration
- se creează tabele pentru users, roles, questions și secvențe, plus alter-uri incremental

## Limitări cunoscute (din cod)

- /api/questions/checkAnswer este GET cu request body
- rezumatul generat nu este persistat în DB (este doar returnat în răspunsul la upload)
- există câteva importuri/utilități în ai-service care nu sunt folosite complet în fluxul minim (ex: sentence-transformers), păstrate pentru extensii

## Structură repository

- ai-service/
  - app.py: FastAPI endpoints și integrarea cu Ollama
  - Dockerfile, requirements.txt

- dodo/
  - src/main/java: Spring Boot API, securitate, servicii, entități
  - src/main/resources: application.properties, migrații Flyway
  - docker.compose: compose pentru Postgres + backend

- docker-compose.yml
  - compose pentru Postgres + Ollama + ai-service
