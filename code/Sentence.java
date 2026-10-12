package com.example;

import java.util.LinkedList;

public class Sentence {

    public String sentence;
    public int label;
    public LinkedList<String> negationScopes;

    public Sentence() {
        this.negationScopes = new LinkedList<>();
    }

}
