package com.bormotov;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

public class DataBaseClass {
    private static final String FIRST_XML="1.xml";
    private static final String SECOND_XML="2.xml";
    private static final String XSL_FILE="/3.xsl";

    private Integer N;

    public Integer getN() {
        return N;
    }

    public void setN(Integer n) {
        N = n;
    }

    private Connection connection;

    public Connection getConnection() {
        return connection;
    }

    private void createTable() throws SQLException{
        try(Statement statement=connection.createStatement()){
            DatabaseMetaData databaseMetadata = connection.getMetaData();
            ResultSet resultSet = databaseMetadata.getTables(null, null, "test", null);
            if (resultSet.next()) {
                statement.executeUpdate("DELETE FROM TEST");
            } else{
                statement.executeUpdate("CREATE TABLE TEST (FIELD INTEGER)");
            }
        }
    }

    public DataBaseClass() throws IOException, SQLException{
        Properties properties=new Properties();
        try(InputStream inputStream=Main.class.getClassLoader().getResourceAsStream("database.properties")){
            properties.load(inputStream);
        }

        String url=properties.getProperty("db.url");
        String username=properties.getProperty("db.username");
        String password=properties.getProperty("db.password");
        Statement statement=null;

        try{
            connection=DriverManager.getConnection(url,username,password);
            createTable();
        }finally {
            if(statement!=null){
                statement.close();
            }
        }
    }

    public void insert() throws SQLException{
        String sql="INSERT INTO TEST (field) values(?)";
        try(PreparedStatement preparedStatement=connection.prepareStatement(sql)){
            for (int i = 1; i <=N ; i++) {
                preparedStatement.setInt(1,i);
                preparedStatement.executeUpdate();
            }
        }
    }

    public String createFirstXml() throws ParserConfigurationException, TransformerException, IOException, SQLException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder  = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        Element root = document.createElement("entries");

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT FIELD FROM TEST");
        while (resultSet.next()) {
            Element entry = document.createElement("entry");
            Element field = document.createElement("field");
            field.setTextContent(resultSet.getString(1));
            entry.appendChild(field);
            root.appendChild(entry);
        }

        resultSet.close();
        statement.close();

        document.appendChild(root);
        String result = xmlToString(document);
        writePath(result, FIRST_XML);
        return result;
    }

    private String xmlToString(Node root) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();

        transformer.transform(new DOMSource(root), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    private  void writePath(String str, String fileName) throws IOException {
        Path path = Paths.get(fileName);
        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
            bufferedWriter.write(str);
        }
        System.out.println("1.xml is saved to: " + path.toAbsolutePath());
    }

    public Path createSecondXml() throws TransformerException, IOException {
        Path path1 = Paths.get(FIRST_XML);
        Path path2 = Paths.get(SECOND_XML);
        InputStream inputXSL = getClass().getResourceAsStream(XSL_FILE);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        StreamSource streamSource = new StreamSource(inputXSL);
        Transformer transformer = transformerFactory.newTransformer(streamSource);

        StreamSource streamSource1 = new StreamSource(path1.toFile());
        StreamResult streamResult = new StreamResult(path2.toFile());
        transformer.transform(streamSource1, streamResult);

        System.out.println("2.xml is saved to:" + path2.toAbsolutePath());
        return path2;
    }

    public long parserXml(Path path) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder  = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(path.toFile());

        NodeList nodeList = doc.getElementsByTagName("entry");
        long sum = 0;

        for(int i = 0; i < nodeList.getLength(); i++) {
            String fieldValue = nodeList.item(i).getAttributes().getNamedItem("field").getNodeValue();
            sum += Integer.parseInt(fieldValue);
        }
        return sum;
    }


}
