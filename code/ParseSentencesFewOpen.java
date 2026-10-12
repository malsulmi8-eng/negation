package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

import dev.langchain4j.model.openai.OpenAiChatModel;

public class ParseSentencesFewOpen {
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
                    new FileWriter(
                            "abstracts_few_Claudeout.txt",
                            true));
//2936
            for (int i = 8106; i <= sentencesMap.size(); i++) {
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
/* 
        String prompt = "You are given a sentence from the biomedical domain. Your task is to determine whether the sentence contains one or more instances of genuine negation.\n"
                + //
                "\n" + //
                "If genuine negation is present, identify the exact textual scope of each negation, including the negation cue itself.\n"
                + //
                "\n" + //
                "1. What counts as genuine negation?\n" + //
                "\n" + //
                "A sentence contains genuine negation when it expresses the non-existence, absence, non-occurrence, or denial of something.\n"
                + //
                "\n" + //
                "Do not decide based only on the presence of words that commonly indicate negation. Always consider the meaning and context of the entire sentence.\n"
                + //
                "\n" + //
                "Negative keywords such as no, not, never, neither, nor, without, cannot, did not, does not, is not, was not, excluded, ruled out, etc. should be annotated only when they genuinely express negation.\n"
                + //
                "\n" + //
                "2. Negative-looking expressions that are NOT genuine negation\n" + //
                "\n" + //
                "Do not annotate an expression merely because it contains a negative-looking word.\n" + //
                "\n" + //
                "Example 1\n" + //
                "\n" + //
                "\"A small amount of adenopathy cannot be completely excluded.\"\n" + //
                "\n" + //
                "Do not annotate this as negation. The statement does not assert that adenopathy is absent. Instead, it indicates that adenopathy remains possible.\n"
                + //
                "\n" + //
                "Example 2\n" + //
                "\n" + //
                "\"Tumor necrosis factor induced slightly c-fos and had almost no effect on c-jun and AP1.\"\n" + //
                "\n" + //
                "Do not annotate \"almost no effect\" as negation. Here, \"almost no\" means very little, rather than complete absence.\n"
                + //
                "\n" + //
                "Example 3\n" + //
                "\n" + //
                "\"Thus, signaling in NK3.3 cells is not always identical with that in primary NK cells.\"\n" + //
                "\n" + //
                "Do not annotate \"not always\" as negation. Here, \"not always\" means sometimes, rather than expressing straightforward negation.\n"
                + //
                "\n" + //
                "Example 4\n" + //
                "\n" + //
                "\"NAC did not significantly affect the spontaneous production of IgE by atopic B cells.\"\n" + //
                "\n" + //
                "If \"not significantly\" means that the effect was small or limited, rather than that there was no effect, do not automatically annotate it as negation. Consider the meaning of the statement in context.\n"
                + //
                "\n" + //
                "Therefore:\n" + //
                "\n" + //
                "Do not rely solely on negative keywords. Determine whether the sentence actually asserts the absence, non-occurrence, or denial of something.\n"
                + //
                "\n" + //
                "3. Negation scope\n" + //
                "\n" + //
                "For every instance of genuine negation, identify its exact textual scope.\n" + //
                "\n" + //
                "The scope must:\n" + //
                "\n" + //
                "contain the negation cue itself;\n" + //
                "contain the text whose meaning is negated;\n" + //
                "be limited to the exact relevant span of the sentence.\n" + //
                "\n" + //
                "Do not include unrelated text outside the negation.\n" + //
                "\n" + //
                "4. Multiple negations\n" + //
                "\n" + //
                "A sentence may contain multiple distinct genuine negations.\n" + //
                "\n" + //
                "When multiple genuine negations are present:\n" + //
                "\n" + //
                "identify each distinct negation scope separately;\n" + //
                "output one [1, scope] entry for each scope;\n" + //
                "preserve the order in which the negation scopes occur in the sentence;\n" + //
                "do not combine independent negation scopes into a single scope.\n" + //
                "\n" + //
                "For example, if a sentence contains two independent negations, output:\n" + //
                "\n" + //
                "[1, first negation scope]\n" + //
                "[1, second negation scope]\n" + //
                "\n" + //
                "A single complex negation construction such as neither...nor should normally be treated as one scope when both parts belong to the same negation.\n"
                + //
                "\n" + //
                "5. Examples\n" + //
                "\n" + //
                "The following examples illustrate the intended annotation behavior. Not Negated means that no genuine negation should be annotated.\n"
                + //
                "\n" + //
                "Not Negated. In nuclear extracts from monocytes or macrophages, induction of NF-KB occurred only if the cells were previously infected with HIV-1.\n"
                + //
                "Negated. PMA treatment, and not retinoic acid treatment of the U937 cells acts in inducing NF-KB expression in the nuclei.\n"
                + //
                "Not Negated. These findings also highlight the potential for an interplay between the Notch and NF-kappaB signaling pathways in the immune system.\n"
                + //
                "Scope: [not retinoic acid treatment of the u937 cells]\n" + //
                "Negated. When U937 cells were infected with HIV-1, no induction of NF-KB factor was detected, whereas high level of progeny virions was produced, suggesting that this factor was not required for viral replication.\n"
                + //
                "Scope 1: [no induction of nf-kb factor was detected]\n" + //
                "Scope 2: [not required for viral replication]\n" + //
                "Not Negated. Positive and negative regulation of immunoglobulin gene expression by a novel B-cell-specific enhancer element.\n"
                + //
                "Not Negated. Little is known of expression of 1,25(OH)2D3 receptor RNA in hematopoietic cells.\n" + //
                "Negated. The Thy-1 gene promoter resembles a \"housekeeping\" promoter in that it is located within a methylation-free island, lacks a canonical TATA box, and displays heterogeneity in the 5'-end termini of the mRNA.\n"
                + //
                "Scope: [lacks a canonical tata box]\n" + //
                "Negated. No group differences in cortisol levels were observed, nor were glucocorticoid receptor number and cortisol levels correlated.\n"
                + //
                "Scope 1: [no group differences in cortisol levels were observed]\n" + //
                "Scope 2: [nor were glucocorticoid receptor number and cortisol levels correlated]\n" + //
                "Not Negated. Immune response of peripheral blood mononuclear cells to HBx-antigen of hepatitis B virus.\n"
                + //
                "Negated. The results were compared with those of 12 menopausal women without diabetes and 12 normal women of childbearing age.\n"
                + //
                "Scope: [without diabetes]\n" + //
                "Negated. FK506 and cyclosporin A block translocation of the cytoplasmic component without affecting synthesis of the nuclear subunit.\n"
                + //
                "Scope: [without affecting synthesis of the nuclear subunit]\n" + //
                "Negated. Phorbol activation was positively modulated by Ca2+ influx while TNF alpha activation was not.\n"
                + //
                "Scope: [tnf alpha activation was not]\n" + //
                "Negated. CONCLUSIONS Hypercortisolaemia does not down-regulate the lymphocyte glucocorticoid receptors in anorexia nervosa and a post-receptor defect might be involved in peripheral tissue resistance to the effects of glucocorticoid hormones in undernutrition.\n"
                + //
                "Scope: [not down-regulate the lymphocyte glucocorticoid receptors in anorexia nervosa]\n" + //
                "Negated. Eed binds to K protein through a domain in its N terminus, but interestingly, this domain is not found in the Drosophila Esc.\n"
                + //
                "Scope: [this domain is not found in the drosophila esc]\n" + //
                "Not Negated. Using homologous recombination, both EKLF alleles in murine embryonic stem (ES) cells were inactivated.\n"
                + //
                "6. Required output\n" + //
                "\n" + //
                "The input sentence will be provided as:\n" + //
                "\n" + //
                "[" + sentence + "]\n" + //
                "\n" + //
                "Return only the required annotation.\n" + //
                "\n" + //
                "If there is one genuine negation\n" + //
                "\n" + //
                "Output:\n" + //
                "\n" + //
                "[1, negation scope]\n" + //
                "\n" + //
                "If there are multiple genuine negations\n" + //
                "\n" + //
                "Output one line for each distinct scope:\n" + //
                "\n" + //
                "[1, negation scope 1]\n" + //
                "[1, negation scope 2]\n" + //
                "...\n" + //
                "\n" + //
                "If there is no genuine negation or no sentence provided\n" + //
                "\n" + //
                "Output:\n" + //
                "\n" + //
                "[0, NULL]\n" + //
                "\n" + //
                "7. Strict output constraint\n" + //
                "\n" + //
                "Do not provide explanations, reasoning, commentary, labels, confidence scores, or any additional text.\n"
                + //
                "\n" + //
                "Output only the required annotation format.";
                */
                String prompt = "You are given a sentence from the biomedical domain. Your task is to determine whether the sentence contains one or more instances of genuine negation.\n"
                + //
                "\n" + //
                "If genuine negation is present, identify the exact textual scope of each negation, including the negation cue itself.\n"
                + //
                "\n" + //
                "1. What counts as genuine negation?\n" + //
                "\n" + //
                "A sentence contains genuine negation when it expresses the non-existence, absence, non-occurrence, or denial of something.\n"
                + //
                "\n" + //
                "Do not decide based only on the presence of words that commonly indicate negation. Always consider the meaning and context of the entire sentence.\n"
                + //
                "\n" + //
                "Negative keywords such as no, not, never, neither, nor, without, cannot, did not, does not, is not, was not, excluded, ruled out, etc. should be annotated only when they genuinely express negation.\n"
                + //
                "\n" + //
                "2. Negative-looking expressions that are NOT genuine negation\n" + //
                "\n" + //
                "Do not annotate an expression merely because it contains a negative-looking word.\n" + //
                "\n" + //
                "Example 1\n" + //
                "\n" + //
                "\"A small amount of adenopathy cannot be completely excluded.\"\n" + //
                "\n" + //
                "Do not annotate this as negation. The statement does not assert that adenopathy is absent. Instead, it indicates that adenopathy remains possible.\n"
                + //
                "\n" + //
                "Example 2\n" + //
                "\n" + //
                "\"Tumor necrosis factor induced slightly c-fos and had almost no effect on c-jun and AP1.\"\n" + //
                "\n" + //
                "Do not annotate \"almost no effect\" as negation. Here, \"almost no\" means very little, rather than complete absence.\n"
                + //
                "\n" + //
                "Example 3\n" + //
                "\n" + //
                "\"Thus, signaling in NK3.3 cells is not always identical with that in primary NK cells.\"\n" + //
                "\n" + //
                "Do not annotate \"not always\" as negation. Here, \"not always\" means sometimes, rather than expressing straightforward negation.\n"
                + //
                "\n" + //
                "Example 4\n" + //
                "\n" + //
                "\"NAC did not significantly affect the spontaneous production of IgE by atopic B cells.\"\n" + //
                "\n" + //
                "If \"not significantly\" means that the effect was small or limited, rather than that there was no effect, do not automatically annotate it as negation. Consider the meaning of the statement in context.\n"
                + //
                "\n" + //
                "Therefore:\n" + //
                "\n" + //
                "Do not rely solely on negative keywords. Determine whether the sentence actually asserts the absence, non-occurrence, or denial of something.\n"
                + //
                "\n" + //
                "3. Negation scope\n" + //
                "\n" + //
                "For every instance of genuine negation, identify its exact textual scope.\n" + //
                "\n" + //
                "The scope must:\n" + //
                "\n" + //
                "contain the negation cue itself;\n" + //
                "contain the text whose meaning is negated;\n" + //
                "be limited to the exact relevant span of the sentence.\n" + //
                "\n" + //
                "Do not include unrelated text outside the negation.\n" + //
                "\n" + //
                "4. Multiple negations\n" + //
                "\n" + //
                "A sentence may contain multiple distinct genuine negations.\n" + //
                "\n" + //
                "When multiple genuine negations are present:\n" + //
                "\n" + //
                "identify each distinct negation scope separately;\n" + //
                "output one [1, scope] entry for each scope;\n" + //
                "preserve the order in which the negation scopes occur in the sentence;\n" + //
                "do not combine independent negation scopes into a single scope.\n" + //
                "\n" + //
                "For example, if a sentence contains two independent negations, output:\n" + //
                "\n" + //
                "[1, first negation scope]\n" + //
                "[1, second negation scope]\n" + //
                "\n" + //
                "A single complex negation construction such as neither...nor should normally be treated as one scope when both parts belong to the same negation.\n"
                + //
                "\n" + //
                "5. Examples\n" + //
                "\n" + //
                "The following examples illustrate the intended annotation behavior. Not Negated means that no genuine negation should be annotated.\n"
                + //
                "\n" + //
                "Not negated. Since a number of microbial genomes have been completely sequenced to date, it is tempting to ask whether the 23rd amino acid is left undiscovered in these genomes\n"
                + //
                "Not Negated. Evidence from co-immunoprecipitation experiments indicates that MSL1 and MSL2 interact directly with each other, a notion supported by the MSL1-MSL2 association at the 35 chromosomal entry sites in the absence of any other MSLs (the preDCC; Figure 1).\n"
                +//
                "Negated. However, no promising candidate for the 23rd amino acid was detected, and only one novel selenoprotein was predicted.\n"
                + //
                "Scope: [no promising candidate for the 23rd amino acid was detected]\n" + //
                "Negated. The most likely explanation is that the 23rd amino acid does not exist, or its distribution on the tree of life is rather limited.\n"
                + //
                "Scope 1: [the 23rd amino acid does not exist, or its distribution on the tree of life is rather limited]\n" + //
                "Scope 2: [not exist]\n" + //
                "Not Negated. However, performance of the tRNA prediction program on an unknown tRNA family, which may have atypical sequence and structure, is unclear, thereby rendering their result inconclusive.\n"
                + //
                "Not Negated. The prediction schema is shown in Figure 2.\n" + //
                "Negated. Actually, tRNASec and tRNAPyl have unusual secondary structures 515 and often escape detection by programs without special consideration.\n"
                + //
                "Scope: [without special consideration]\n" + //
                "Negated. At least, only one experimentally-confirmed example from a pathogenic strain of Escherichia coli 23, whose genome is not yet determined, does not obey this rule.\n"
                + //
                "Scope 1: [whose genome is not yet determined]\n" + //
                "Scope 2: [not obey this rule]\n" + //
                "Not Negated. stimates for the prior odds ratio of interaction vary.\n"
                + //
                "Negated. If an iORF matched any protein-coding genes (E-value < 10-3) and their reading frames did not coincide, the iORF was discarded.\n"
                + //
                "Scope: [not coincide]\n" + //
                "Negated. s317,1,1,Therefore, in an ideal situation the presence or absence of the 21 residues in alignments will be synchronized.\n"
                + //
                "Scope: [absence of the 21 residues in alignments]\n" + //
                "Negated. Protein pairs that have a posterior odds of interaction above 1 are more likely to interact than not to interact, thus providing an obvious threshold to predict interacting proteins.\n"
                + //
                "Scope: [not to interact]\n" + //
                "Negated. In other words, species-specific readthrough genes are not in the scope of this study.\n"
                + //
                "Scope: [not in the scope of this study]\n" + //
                "Negated. Whether the codon aligned to the inframe stop codon is a nonsense codon or not was neglected at this stage.\n"
                + //
                "Scope: [not]\n" + //
                "Not Negated. s288,0,0,Eligible iORFs were then clustered using BLASTCLUST with score density 0.5 and minimum length coverage 0.6.\n"
                + //
                "6. Required output\n" + //
                "\n" + //
                "The input sentence will be provided as:\n" + //
                "\n" + //
                "[" + sentence + "]\n" + //
                "\n" + //
                "Return only the required annotation.\n" + //
                "\n" + //
                "If there is one genuine negation\n" + //
                "\n" + //
                "Output:\n" + //
                "\n" + //
                "[1, negation scope]\n" + //
                "\n" + //
                "If there are multiple genuine negations\n" + //
                "\n" + //
                "Output one line for each distinct scope:\n" + //
                "\n" + //
                "[1, negation scope 1]\n" + //
                "[1, negation scope 2]\n" + //
                "...\n" + //
                "\n" + //
                "If there is no genuine negation or no sentence provided\n" + //
                "\n" + //
                "Output:\n" + //
                "\n" + //
                "[0, NULL]\n" + //
                "\n" + //
                "7. Strict output constraint\n" + //
                "\n" + //
                "Do not provide explanations, reasoning, commentary, labels, confidence scores, or any additional text.\n"
                + //
                "\n" + //
                "Output only the required annotation format.";
        String response = model.chat(prompt);
        if(response == null) {
            return "[0, NULL]";
        }
        return response;
    }

}
