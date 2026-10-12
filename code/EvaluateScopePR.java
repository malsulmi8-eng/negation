package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EvaluateScopePR {
    public static void main(String[] args) {
        HashMap<Integer, Sentence> sentencesMap = new HashMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();

            // parse the xml file
            org.w3c.dom.Document document = builder.parse(
                    "abstracts.xml");

            document.getDocumentElement().normalize();

            // get all the sentences
            NodeList sentences = document.getElementsByTagName("sentence");

            // iterate through the sentences and print them
            for (int i = 0; i < sentences.getLength(); i++) {
                org.w3c.dom.Element sentence = (org.w3c.dom.Element) sentences.item(i);
                Sentence sentenceObj = new Sentence();
                sentenceObj.sentence = sentence.getTextContent();

                // let's check if the sentence has a "xcope" tag
                NodeList xcope = sentence.getElementsByTagName("xcope");
                boolean hasNegation = false;

                if (xcope.getLength() > 0) {
                    for (int j = 0; j < xcope.getLength(); j++) {
                        org.w3c.dom.Element xcopeElement = (org.w3c.dom.Element) xcope.item(j);
                        if (xcopeElement.getElementsByTagName("cue").getLength() > 0) {
                            if (xcopeElement.getElementsByTagName("cue").item(0).getAttributes().getNamedItem("type")
                                    .getTextContent().equals("negation")) {
                                hasNegation = true;
                                sentenceObj.label = 1;
                                sentenceObj.negationScopes.add(xcopeElement.getTextContent().toLowerCase().trim());
                            }
                        }
                    }
                }

                if (!hasNegation) {
                    sentenceObj.label = 0;
                }
                sentencesMap.put(i + 1, sentenceObj);
            }
            System.out.println("Total sentences in map: " + sentencesMap.size());

            BufferedReader reader = new BufferedReader(
                    new FileReader("abstracts_one_Claudeout_fixed.txt"));
            int predictedTokens = 0;
            int correctlyClassified = 0;
            int allTokens = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\[");
                if (parts.length < 2) {
                    System.out.println("Invalid line format: " + line);
                    continue;
                } else if (parts.length == 2) {
                    int sentenceIndex = Integer.parseInt(parts[0].split(",")[0].replace("s", "").trim());
                    Sentence sentenceObj = sentencesMap.get(sentenceIndex);
                    int actualLabel = Integer.parseInt(parts[0].split(",")[1].trim());
                    int predictedLabel = Integer.parseInt(parts[1].split(",")[0].trim());
                    if (predictedLabel == 1 && actualLabel == 1) {
                        String predictedScope = parts[1].replaceFirst("1,", "").replaceFirst("\\]", "").trim();
                        // let's split the predicted scope into tokens
                        String[] predictedScopeTokens = predictedScope.toLowerCase().trim().split("\\s+");
                        String[] actualScopeTokens = sentenceObj.negationScopes.stream()
                                .flatMap(s -> java.util.Arrays.stream(s.split("\\s+"))).toArray(String[]::new);
                        allTokens += actualScopeTokens.length;
                        for (String token : predictedScopeTokens) {
                            predictedTokens++;
                            for (String actualToken : actualScopeTokens) {
                                if (actualToken.contains(token)) {
                                    correctlyClassified++;
                                    break;
                                }
                            }
                            // if (sentenceObj.negationScopes.contains(token)) {
                            // correctlyClassified++;
                            // }
                        }
                    }

                } else {
                    int sentenceIndex = Integer.parseInt(parts[0].split(",")[0].replace("s", "").trim());
                    Sentence sentenceObj = sentencesMap.get(sentenceIndex);
                    int actualLabel = Integer.parseInt(parts[0].split(",")[1].trim());
                    if (actualLabel == 1) {
                        String[] actualScopeTokens = sentenceObj.negationScopes.stream()
                                .flatMap(s -> java.util.Arrays.stream(s.split("\\s+"))).toArray(String[]::new);
                        allTokens += actualScopeTokens.length;

                        for (int i = 1; i < parts.length; i++) {
                            String predictedScope = parts[i].replaceFirst("1,", "").replaceFirst("\\]", "").trim();
                            String[] predictedScopeTokens = predictedScope.toLowerCase().trim().split("\\s+");

                            for (String token : predictedScopeTokens) {
                                predictedTokens++;
                                for (String actualToken : actualScopeTokens) {
                                    if (actualToken.contains(token)) {
                                        correctlyClassified++;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                }

            }
            // let's compute precision and recall for scope detection
            double precision = (double) correctlyClassified / predictedTokens;
            double recall = (double) correctlyClassified / allTokens;
            double f1 = 2 * (precision * recall) / (precision + recall);
            System.out.println("Total tokens in actual scopes: " + allTokens);
            System.out.println("Total tokens in predicted scopes: " + predictedTokens);
            System.out.println("Correctly classified tokens: " + correctlyClassified);
            System.out.println("Precision: " + precision * 100 + "%");
            System.out.println("Recall: " + recall * 100 + "%");
            System.out.println("F1 score: " + f1 * 100 + "%");

        } catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
