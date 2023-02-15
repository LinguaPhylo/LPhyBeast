package lphybeast.alignment;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.parser.XMLProducer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class AlignmentEditor {




    public static void main(String[] args) {

        String xmlFilePath = args[0];

        File xmlF = new File(xmlFilePath);

        Document doc = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(xmlF);
            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Cannot find/parse XML file containing alignment " + xmlFilePath);
        }

        StringBuilder stringBuilder = new StringBuilder();

        // may multiple <data ... ></data>
        NodeList dataNodeList = doc.getElementsByTagName("data");
        for (int i = 0; i < dataNodeList.getLength(); i++) {
            // <data ... ></data>
            Node datNod = dataNodeList.item(i);

            if (datNod.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) datNod;
                String id = element.getAttribute("id");
                String dt = element.getAttribute("dataType");

                System.out.println("\n<data id=" + id + " spec=" + element.getAttribute("spec") + " dataType=" + dt);

                List<Sequence> seqList = new ArrayList<>();

                NodeList seqNL = element.getElementsByTagName("sequence");
                for (int s = 0; s < seqNL.getLength(); s++) {
                    Node seqNod = dataNodeList.item(s);
//TODO
                    Sequence seq = new Sequence("0human", "AAAACCCCGGGGAAAA");

                    seqList.add(seq);
                }
                System.out.println("Processing " + seqList.size() + " sequences ...");

                //TODO modify seq


                Alignment data = new Alignment();
                data.initByName("sequence", seqList, "dataType", dt );
                data.setID(id);

                String datXML = new XMLProducer().toXML(data);
                stringBuilder.append(datXML).append("\n");
            }
        }

        String outFP = xmlFilePath.replace(".xml", "-new.xml");
        File outF = new File(outFP);

        PrintWriter writer;
        try {
            FileWriter fileWriter = new FileWriter(outF);
            writer = new PrintWriter(fileWriter);
            writer.println(stringBuilder);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        writer.close();
    }

}
