package com.bormotov;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        DataBaseClass dataBaseClass = null;
        long arifmeticSum = 0;
        try {
            dataBaseClass = new DataBaseClass();
            dataBaseClass.setN(10000);

            dataBaseClass.insert();
            dataBaseClass.createFirstXml();
            Path path = dataBaseClass.createSecondXml();
            arifmeticSum = dataBaseClass.parserXml(path);
            if(dataBaseClass.getConnection() != null)
                dataBaseClass.getConnection().close();
        } catch (IOException | SAXException | SQLException | ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }

        System.out.println("arifmeticSum is " + arifmeticSum );
    }
}
