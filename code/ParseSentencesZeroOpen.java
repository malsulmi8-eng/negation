package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

import dev.langchain4j.model.openai.OpenAiChatModel;

public class ParseSentencesZeroOpen {
    public static void main(String[] args) {

        try {
            String apiKey = "";

            // Build the Ollama chat model pointing to localhost
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .baseUrl("https://openrouter.ai/api/v1")
                    .apiKey(apiKey)
                    .modelName("anthropic/claude-sonnet-5.5")
                    //.modelName("google/gemini-3.7-flash")
                    .build();

            // read xml file and parse sentences
            HashMap<Integer, Sentence> sentencesMap = new HashMap<>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;

            builder = factory.newDocumentBuilder();
            // parse the xml file
            org.w3c.dom.Document document = builder.parse(
                    "abstracts.xml");

            document.getDocumentElement().normalize();

            // get all the sentences
            NodeList sentences = document.getElementsByTagName("sentence");
            int negationCount = 0;
            int xcopeCount = 0;
            int cueCount = 0;

            // iterate through the sentences and print them
            for (int i = 0; i < sentences.getLength(); i++) {
                org.w3c.dom.Element sentence = (org.w3c.dom.Element) sentences.item(i);
                Sentence sentenceObj = new Sentence();
                sentenceObj.sentence = sentence.getTextContent();
                // System.out.println("sentence " + (i + 1) + " = " +
                // sentence.getTextContent());
                // let's check if the sentence has a "xcope" tag
                NodeList xcope = sentence.getElementsByTagName("xcope");
                xcopeCount += xcope.getLength();
                boolean hasNegation = false;
                int xcopeWithNegationCount = 0;
                if (xcope.getLength() > 0) {
                    for (int j = 0; j < xcope.getLength(); j++) {
                        org.w3c.dom.Element xcopeElement = (org.w3c.dom.Element) xcope.item(j);
                        if (xcopeElement.getElementsByTagName("cue").getLength() > 0) {
                            cueCount++;
                            if (xcopeElement.getElementsByTagName("cue").item(0).getAttributes().getNamedItem("type")
                                    .getTextContent().equals("negation")) {
                                hasNegation = true;
                                sentenceObj.label = 1;
                                xcopeWithNegationCount++;
                                negationCount++;
                                // System.out.println("cue " + (xcopeWithNegationCount) + " = " +
                                // xcopeElement.getElementsByTagName("cue").item(0).getTextContent());
                                // let's print the xcope text content
                                // System.out.println("sentence " + (i + 1) + ": xcope " +
                                // (xcopeWithNegationCount) + " = " + xcopeElement.getTextContent());
                                sentenceObj.negationScopes.add(xcopeElement.getTextContent());
                            }
                        }
                    }
                }

                if (!hasNegation) {
                    sentenceObj.label = 0;
                }
                sentencesMap.put(i + 1, sentenceObj);
                /// here we will ask the llm to predict the label for the sentence and append it
                /// to the textToAppend variable
                // writer.write(textToAppend + "\n");
                // System.out.println();
            }
            System.out.println("Total sentences: " + sentences.getLength());
            System.out.println("Total xcope tags found: " + xcopeCount);
            System.out.println("Total cue tags found: " + cueCount);
            System.out.println("Total sentences with negation: " + negationCount);

            System.out.println("Total sentences in map: " + sentencesMap.size());

            BufferedWriter writer = new BufferedWriter(
                    new FileWriter("abstracts_Claudeout.txt",
                            true));

            for (int i = 8932; i <= sentencesMap.size(); i++) {
                Sentence sentenceObj = sentencesMap.get(i);
                String llmResponse = getLLMResponse(sentenceObj.sentence, model);
                writer.write("s" + i + "," + sentenceObj.label + "," + llmResponse.trim().replace("\n", "") + "\n");
                System.out.println("s" + i + "," + sentenceObj.label + "," + llmResponse.trim().replace("\n", ""));
                writer.flush();
            }
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String getLLMResponse(String sentence, OpenAiChatModel model) {
        // Here you would call your LLM API with the prompt and get the response

        String prompt = "You will be given a sentence from the biomedical domain. Determine whether the sentence contains one or more instances of negation. For each negation, identify its exact negation scope.\n"
                + //
                "\n" + //
                "Rules:\n" + //
                "\n" + //
                "Output 1 if the sentence contains negation.\n" + //
                "Output 0 if the sentence contains no negation.\n" + //
                "For each negation, provide the exact text span representing its negation scope, including the negation cue itself (e.g., no, not, without, never, etc.).\n"
                + //
                "If the sentence contains multiple distinct negation scopes, output a separate [1, negation scope] for each scope.\n"
                + //
                "If there is no negation, output [0, NULL].\n" + //
                "Preserve the exact wording and order of the negation scopes as they appear in the sentence.\n" + //
                "\n" + //
                "The sentence is:\n" + //
                "\n" + //
                "[" + sentence + "]\n" + //
                "\n" + //
                "Output only in one of the following formats:\n" + //
                "\n" + //
                "For one negation:\n" + //
                "[1, negation scope]\n" + //
                "\n" + //
                "For multiple negations:\n" + //
                "[1, negation scope 1]\n" + //
                "[1, negation scope 2]\n" + //
                "...\n" + //
                "\n" + //
                "For no negation:\n" + //
                "[0, NULL]\n" + //
                "\n" + //
                "Do not provide any explanation, reasoning, labels, or additional text. Follow the specified output format exactly.";
        // System.out.println(prompt);
        

        String response = model.chat(prompt);
        if (response == null) {
            return "[0, NULL]";
        }
        return response;
    }

}
