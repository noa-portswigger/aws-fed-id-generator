package com.resare.aws_fed_id.generator;

public class Example {

    /**
     * This example generates and prints a token to standard out with the default AWS Credentials with the audience
     * "example.com".
     */
    public static void main(String[] args) {
        var generator = new TokenGenerator();
        System.out.println(generator.generate("example.com"));
    }
}