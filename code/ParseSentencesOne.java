package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

import dev.langchain4j.model.openai.OpenAiChatModel;

public class ParseSentencesOne {
    public static void main(String[] args) {

        try {
            String apiKey = "";

            OpenAiChatModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-5.6-luna")
                    .build();

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
                    new FileWriter("abstracts_one_GPTout.txt",
                            true));

            for (int i = 6872; i <= sentencesMap.size(); i++) {
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

        String prompt = "You will be given a sentence from the biomedical domain. Determine whether the sentence contains one or more instances of genuine negation. If negation is present, identify the exact scope of each negation, including the negation cue itself.\n"
                + //
                "\n" + //
                "1. What counts as negation?\n" + //
                "\n" + //
                "A sentence should be annotated as negative when it expresses the non-existence, absence, non-occurrence, or denial of something.\n"
                + //
                "\n" + //
                "The presence of a word that is commonly associated with negation does not automatically mean that the sentence contains negation. Carefully consider the meaning and context of the entire sentence.\n"
                + //
                "\n" + //
                "Negative keywords such as no, not, never, neither, nor, without, cannot, did not, does not, is not, was not, excluded, ruled out, etc. should only be annotated when they genuinely express negation.\n"
                + //
                "\n" + //
                "2. Negative-looking words that do NOT express negation\n" + //
                "\n" + //
                "Do not annotate a negative keyword when it does not express actual negation.\n" + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"A small amount of adenopathy cannot be completely excluded.\"\n" + //
                "\n" + //
                "Here, \"cannot\" does not indicate that adenopathy is absent. Rather, the statement indicates that adenopathy remains possible, so it should not be annotated as negation.\n"
                + //
                "\n" + //
                "Similarly:\n" + //
                "\n" + //
                "\"Tumor necrosis factor induced slightly c-fos and had almost no effect on c-jun and AP1.\"\n" + //
                "\n" + //
                "Here, \"almost no\" means very little, rather than complete absence. Do not annotate it as negation.\n"
                + //
                "\n" + //
                "\"Thus, signaling in NK3.3 cells is not always identical with that in primary NK cells.\"\n" + //
                "\n" + //
                "Here, \"not always\" means sometimes, rather than expressing straightforward negation. Do not annotate it as negation.\n"
                + //
                "\n" + //
                "\"NAC did not significantly affect the spontaneous production of IgE by atopic B cells.\"\n" + //
                "\n" + //
                "If \"not significantly\" means that the effect was small or limited rather than that there was no effect, do not automatically annotate it as negation. Consider the meaning of the statement in context.\n"
                + //
                "\n" + //
                "Therefore, do not rely solely on negative keywords. Determine whether the sentence actually asserts the absence or non-occurrence of something.\n"
                + //
                "\n" + //
                "3. Negation scope\n" + //
                "\n" + //
                "For every genuine negation, identify its exact textual scope.\n" + //
                "\n" + //
                "The negation scope must include the negation cue itself.\n" + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"Single AP of the abdomen does not reveal the etiology of the patient's pain.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "does not reveal the etiology of the patient's pain\n" + //
                "\n" + //
                "\"The previously demonstrated focus within the mid pole of the right kidney is not seen on the current study.\"\n"
                + //
                "\n" + //
                "Scope:\n" + //
                "is not seen on the current study\n" + //
                "\n" + //
                "4. Negative determiners in the subject\n" + //
                "\n" + //
                "When a negative determiner such as no or neither occurs in the subject of a sentence, its scope generally extends over the entire sentence/clause.\n"
                + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"No instance of microsatellite instability was observed in a total of 880 comparisons of granulocyte and T-cell DNA.\"\n"
                + //
                "\n" + //
                "Scope:\n" + //
                "No instance of microsatellite instability was observed in a total of 880 comparisons of granulocyte and T-cell DNA\n"
                + //
                "\n" + //
                "\"Neither of these proteins bound in vitro to EBS1 or EBS2.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "Neither of these proteins bound in vitro to EBS1 or EBS2\n" + //
                "\n" + //
                "5. Negative conjunctions\n" + //
                "\n" + //
                "Negative conjunctions such as neither...nor generally have scope over the syntactic unit whose members they coordinate.\n"
                + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"In contrast, sodium salicylate (1 mM) inhibited neither adhesion nor expression of these adhesion molecules.\"\n"
                + //
                "\n" + //
                "Scope:\n" + //
                "neither adhesion nor expression of these adhesion molecules\n" + //
                "\n" + //
                "If a complex negative construction occurs in the subject, its scope may extend over the entire sentence.\n"
                + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"Neither the SNE nor the SEE motif responded to phosphatase inhibition by okadaic acid.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "Neither the SNE nor the SEE motif responded to phosphatase inhibition by okadaic acid\n" + //
                "\n" + //
                "Treat a single complex negative construction such as neither...nor as one negation scope, not as two separate scopes.\n"
                + //
                "\n" + //
                "6. Negation expressed by prepositions\n" + //
                "\n" + //
                "Negative prepositions such as without generally have scope over the following noun phrase or syntactic unit.\n"
                + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"Mildly hyperinflated lungs without focal opacity.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "without focal opacity\n" + //
                "\n" + //
                "7. Negation keywords without an explicit scope\n" + //
                "\n" + //
                "Sometimes a negative keyword clearly expresses negation, but the thing being negated is not explicitly present in the sentence.\n"
                + //
                "\n" + //
                "In such cases, annotate the negative keyword itself as the scope.\n" + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"Negative chest radiograph.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "Negative\n" + //
                "\n" + //
                "\"Otherwise negative.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "negative\n" + //
                "\n" + //
                "8. Elliptical constructions\n" + //
                "\n" + //
                "In elliptical sentences, annotate the negative keyword even when the verb or phrase that would normally complete its scope is omitted.\n"
                + //
                "\n" + //
                "For example:\n" + //
                "\n" + //
                "\"This decrease was seen in patients who responded to the therapy as well as in those who did not.\"\n"
                + //
                "\n" + //
                "Scope:\n" + //
                "not\n" + //
                "\n" + //
                "Do not infer and add omitted words that are not explicitly present in the sentence.\n" + //
                "\n" + //
                "For comparison:\n" + //
                "\n" + //
                "\"This decrease was seen in patients who responded to the therapy as well as in those who did not respond to the therapy.\"\n"
                + //
                "\n" + //
                "Scope:\n" + //
                "not respond to the therapy\n" + //
                "\n" + //
                "Always annotate only the text that is actually present in the input sentence.\n" + //
                "\n" + //
                "9. \"Rule out\" and \"exclude\"\n" + //
                "\n" + //
                "Pay special attention to verbs such as rule out and exclude, because their meaning depends on the grammatical construction.\n"
                + //
                "\n" + //
                "In imperative constructions, they generally indicate uncertainty/hedging, not negation:\n" + //
                "\n" + //
                "\"Rule out pneumonia.\"\n" + //
                "\n" + //
                "Do not annotate this as negation because pneumonia remains a possibility and the sentence recommends an action to determine whether it is present.\n"
                + //
                "\n" + //
                "In passive constructions, however, they can express genuine negation:\n" + //
                "\n" + //
                "\"The possibility of lung cancer is ruled out.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "is ruled out\n" + //
                "\n" + //
                "\"Myelomeningocele is excluded.\"\n" + //
                "\n" + //
                "Scope:\n" + //
                "is excluded\n" + //
                "\n" + //
                "If the passive construction itself is negated, it expresses uncertainty/hedging rather than negation:\n"
                + //
                "\n" + //
                "\"It is not ruled out that the ureterocele opens into the vagina.\"\n" + //
                "\n" + //
                "Do not annotate this as negation because the meaning is that the possibility remains.\n" + //
                "\n" + //
                "\"Early infiltrate is not excluded.\"\n" + //
                "\n" + //
                "Do not annotate this as negation for the same reason.\n" + //
                "\n" + //
                "10. Multiple negations\n" + //
                "\n" + //
                "A sentence may contain multiple distinct negation scopes.\n" + //
                "\n" + //
                "When multiple genuine negations are present, output one [1, scope] entry for each distinct negation scope, in the order in which they occur in the sentence.\n"
                + //
                "\n" + //
                "For example, if a sentence contains two independent negations, output:\n" + //
                "\n" + //
                "[1, first negation scope]\n" + //
                "[1, second negation scope]\n" + //
                "\n" + //
                "Do not combine separate negation scopes into one scope.\n" + //
                "\n" + //
                "However, a single complex negation construction such as neither...nor should normally be treated as one scope when both parts belong to the same negation.\n"
                + //
                "\n" + //
                "11. General annotation principles\n" + //
                "Annotate semantic negation, not merely negative words.\n" + //
                "The negation scope must include the negation cue.\n" + //
                "Preserve the exact wording from the input sentence.\n" + //
                "Do not add words that are not explicitly present in the sentence.\n" + //
                "Use syntactic structure and sentence meaning to determine the scope.\n" + //
                "Consider the entire sentence when deciding whether a negative keyword genuinely expresses negation.\n"
                + //
                "Do not annotate speculative or hedging statements as negation merely because they contain a negative keyword.\n"
                + //
                "Do not treat words such as almost no, not always, not significantly, etc. as negation when they function as expressions of degree, frequency, or qualification rather than genuine absence or denial.\n"
                + //
                "If there are multiple genuine negations, provide a separate output for each scope.\n" + //
                "Preserve the order of the scopes as they appear in the sentence.\n" + //
                "Output format\n" + //
                "\n" + //
                "The sentence is:\n" + //
                "\n" + //
                "[" + sentence + "]\n" + //
                "\n" + //
                "If there is one genuine negation, output:\n" + //
                "\n" + //
                "[1, negation scope]\n" + //
                "\n" + //
                "If there are multiple genuine negations, output one line for each:\n" + //
                "\n" + //
                "[1, negation scope 1]\n" + //
                "[1, negation scope 2]\n" + //
                "...\n" + //
                "\n" + //
                "If there is no genuine negation, output:\n" + //
                "\n" + //
                "[0, NULL]\n" + //
                "\n" + //
                "Do not provide any explanation, reasoning, commentary, labels, or additional text. Output only the required format.";
        // System.out.println(prompt);

        String response = model.chat(prompt);
        return response;
    }

}
