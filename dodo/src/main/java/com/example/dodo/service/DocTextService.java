package com.example.dodo.service;

import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DocTextService {
    public String extract(InputStream input) {
        try {
            var handler = new org.xml.sax.helpers.DefaultHandler();
            var md = new org.apache.tika.metadata.Metadata();
            var parser = new org.apache.tika.parser.AutoDetectParser();
            var ctx = new org.apache.tika.parser.ParseContext();

            var body = new org.apache.tika.sax.BodyContentHandler(-1);
            input.mark(Integer.MAX_VALUE);
            parser.parse(input, body, md, ctx);
            var raw = body.toString();

            var text = raw.replace("\r", "\n")
                    .replaceAll("[\\t\\u00A0]+", " ")
                    .replaceAll(" +", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();
            return text;
        } catch (Exception e) {
            throw new RuntimeException("extract_failed", e);
        }
    }
}

