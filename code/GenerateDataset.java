package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GenerateDataset {
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

            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    "abstracts_dataset.txt", true));

            for (int i = 1; i <= sentencesMap.size(); i++) {
                Sentence sentenceObj = sentencesMap.get(i);
                writer.write("s" + i + "," + sentenceObj.label + "," + sentenceObj.negationScopes.size() + ","
                        + sentenceObj.sentence.trim());
                if (sentenceObj.negationScopes.size() > 0) {
                    writer.write(",[" + String.join("][", sentenceObj.negationScopes) + "]");
                }
                writer.write("\n");
                writer.flush();
            }
            writer.close();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
