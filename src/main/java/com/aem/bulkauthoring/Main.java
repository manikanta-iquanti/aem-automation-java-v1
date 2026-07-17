package com.aem.bulkauthoring;

import com.aem.bulkauthoring.analyzer.BlueprintAnalyzer;
import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.BlueprintComponent;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        File json =
                new File("input/blueprint/page.json");

        Blueprint blueprint =
                new BlueprintAnalyzer().analyze(json);

        System.out.println();

        System.out.println("Components Found : "
                + blueprint.getComponents().size());

        System.out.println();

        for (BlueprintComponent c : blueprint.getComponents()) {

            System.out.println("--------------------------------");

            System.out.println(c.getName());

            System.out.println(c.getResourceType());

            System.out.println(c.getPath());

            c.getProperties().forEach(p ->
                    System.out.println(
                            p.getName() + " = " + p.getValue()));
        }

    }

}