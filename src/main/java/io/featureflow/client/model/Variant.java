package io.featureflow.client.model;

/**
 * Created by oliver on 26/05/2016.
 *
 * A variant is a variant of a feature and at its simplest is ON or OFF but can be any combination of values rad,blue,green,big,small etc
 * A variant is targeted at an Audience then equally split based on its variant split and a equally distributed generated Hash value
 *
 */
public class Variant {
    public Variant() {}

    public Variant(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public static final String off = "off";
    public static final String on = "on";
    public String key; //unique key - true/false/blue/green or another feature
    public String name; //the value of the variant

}