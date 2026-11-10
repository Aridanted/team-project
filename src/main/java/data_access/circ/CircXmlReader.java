package data_access.circ;

import entity.circuit.Point;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import use_case.import_circuit.CircuitParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the XML of a .circ file into a {@link RawCircuit} for its main circuit. Knows the file's
 * element structure but nothing about what components mean. Unknown elements are ignored.
 */
final class CircXmlReader {

    private static final Pattern LOCATION = Pattern.compile("\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)");

    private static final ErrorHandler FAIL_SILENTLY = new ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    };

    RawCircuit read(InputStream xml) throws CircuitParseException {
        Element project = parseSecurely(xml).getDocumentElement();
        if (!"project".equals(project.getTagName())) {
            throw Diagnostics.notACircuitFile();
        }
        Map<String, String> libraries = new HashMap<>();
        String mainName = null;
        List<Element> circuits = new ArrayList<>();
        for (Element child : childElements(project)) {
            switch (child.getTagName()) {
                case "lib" -> libraries.put(child.getAttribute("name"), child.getAttribute("desc"));
                case "main" -> mainName = child.getAttribute("name");
                case "circuit" -> circuits.add(child);
                default -> {
                    // options, mappings, toolbar, vhdl, ... are irrelevant here
                }
            }
        }
        Element circuit = mainCircuit(circuits, mainName);
        return new RawCircuit(circuit.getAttribute("name"),
                components(circuit, libraries), wires(circuit));
    }

    private static Document parseSecurely(InputStream xml) throws CircuitParseException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(FAIL_SILENTLY);
            return builder.parse(xml);
        } catch (SAXException | IOException e) {
            throw Diagnostics.notACircuitFile();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("XML parser does not support secure processing", e);
        }
    }

    private static Element mainCircuit(List<Element> circuits, String mainName)
            throws CircuitParseException {
        if (circuits.isEmpty()) {
            throw Diagnostics.noCircuit();
        }
        return circuits.stream()
                .filter(circuit -> circuit.getAttribute("name").equals(mainName))
                .findFirst()
                .orElse(circuits.get(0));
    }

    private static List<RawComponent> components(Element circuit, Map<String, String> libraries)
            throws CircuitParseException {
        List<RawComponent> components = new ArrayList<>();
        for (Element comp : childElements(circuit, "comp")) {
            String library = libraryOf(comp, libraries);
            String name = comp.getAttribute("name");
            components.add(new RawComponent(library, name, location(comp.getAttribute("loc"), name),
                    attributes(comp)));
        }
        return components;
    }

    private static String libraryOf(Element comp, Map<String, String> libraries)
            throws CircuitParseException {
        if (!comp.hasAttribute("lib")) {
            return "";
        }
        String library = libraries.get(comp.getAttribute("lib"));
        if (library == null) {
            throw Diagnostics.malformed("library " + comp.getAttribute("lib"));
        }
        return library;
    }

    private static Map<String, String> attributes(Element comp) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (Element attribute : childElements(comp, "a")) {
            String value = attribute.hasAttribute("val")
                    ? attribute.getAttribute("val")
                    : attribute.getTextContent();
            attributes.put(attribute.getAttribute("name"), value);
        }
        return attributes;
    }

    private static List<RawCircuit.RawWire> wires(Element circuit) throws CircuitParseException {
        List<RawCircuit.RawWire> wires = new ArrayList<>();
        for (Element wire : childElements(circuit, "wire")) {
            wires.add(new RawCircuit.RawWire(location(wire.getAttribute("from"), "wire"),
                    location(wire.getAttribute("to"), "wire")));
        }
        return wires;
    }

    private static Point location(String text, String owner) throws CircuitParseException {
        Matcher matcher = LOCATION.matcher(text);
        if (!matcher.matches()) {
            throw Diagnostics.malformed("location \"" + text + "\" of " + owner);
        }
        return new Point(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private static List<Element> childElements(Element parent) {
        List<Element> elements = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) children.item(i));
            }
        }
        return elements;
    }

    private static List<Element> childElements(Element parent, String tagName) {
        return childElements(parent).stream().filter(e -> e.getTagName().equals(tagName)).toList();
    }
}
