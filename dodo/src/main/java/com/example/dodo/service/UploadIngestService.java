package com.example.dodo.service;

import com.example.dodo.entities.UploadResultDto;
import com.example.dodo.entities.Question;
import com.example.dodo.repository.QuestionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadIngestService {
    private final QuestionsRepository repo;
    public UploadIngestService(QuestionsRepository repo){ this.repo = repo; }

    @Transactional
    public int saveQuestions(UploadResultDto dto, String domain) {
        if (dto == null || dto.getQuizzes() == null) return 0;
        int saved = 0;
        for (var qd : dto.getQuizzes()) {
            var opts = qd.getOptions();
            if (opts == null || opts.size() < 4) continue;
            int idx = qd.getAnswer_index();
            if (idx < 0 || idx >= opts.size()) idx = 0;
            repo.save(new Question(
                    qd.getQuestion(),
                    opts.get(0), opts.get(1), opts.get(2), opts.get(3),
                    opts.get(idx),
                    domain
            ));
            saved++;
        }
        return saved;
    }
}
