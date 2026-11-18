package com.ferbo.consumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

public class AddendaInjector {

    private static final String CFDI_NS_40 = "http://www.sat.gob.mx/cfd/4";

    /**
     * Agrega una Addenda a un CFDI timbrado. Si el CFDI ya trae una Addenda,
     * lanza IllegalStateException.
     */
    public static String agregarAddenda(String xmlCfdiTimbrado, String xmlContenidoAddenda) throws Exception {
        return agregarAddenda(xmlCfdiTimbrado, xmlContenidoAddenda, false);
    }

    /**
     * @param reemplazarExistente si es true, reemplaza una Addenda existente;
     *                            si es false, lanza IllegalStateException.
     */
    public static String agregarAddenda(String xmlCfdiTimbrado,
                                        String xmlContenidoAddenda,
                                        boolean reemplazarExistente) throws Exception {
        if (xmlCfdiTimbrado == null || xmlCfdiTimbrado.trim().isEmpty()) {
            throw new IllegalArgumentException("El XML del CFDI es nulo o está vacío");
        }
        if (xmlContenidoAddenda == null || xmlContenidoAddenda.trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido de la addenda es nulo o está vacío");
        }

        DocumentBuilder builder = crearBuilderSeguro();

        // 1. Parsear el CFDI timbrado
        Document docCfdi = builder.parse(new InputSource(new StringReader(xmlCfdiTimbrado)));
        Element root = docCfdi.getDocumentElement();

        // 2. Validar que sea un Comprobante CFDI 4.0 y tomar su namespace y prefijo reales
        String cfdiNamespace = root.getNamespaceURI();
        if (!"Comprobante".equals(root.getLocalName()) || !CFDI_NS_40.equals(cfdiNamespace)) {
            throw new IllegalArgumentException(
                    "El documento no es un cfdi:Comprobante 4.0 (namespace " + CFDI_NS_40 + ")");
        }
        String prefijo = root.getPrefix();
        String nombreAddenda = (prefijo == null || prefijo.isEmpty()) ? "Addenda" : prefijo + ":Addenda";

        // 3. Revisar si ya existe una Addenda (hijo directo del Comprobante)
        Node existente = buscarHijo(root, cfdiNamespace, "Addenda");
        if (existente != null) {
            if (!reemplazarExistente) {
                throw new IllegalStateException("El CFDI ya contiene un nodo cfdi:Addenda");
            }
            root.removeChild(existente);
        }

        // 4. Parsear el contenido de la addenda del cliente
        Document docAddenda = builder.parse(new InputSource(new StringReader(xmlContenidoAddenda)));
        Element contenido = docAddenda.getDocumentElement();

        // Protección: error común, pasar un CFDI completo en lugar del nodo de la addenda
        if ("Comprobante".equals(contenido.getLocalName())) {
            throw new IllegalArgumentException(
                    "xmlContenidoAddenda debe ser solo el nodo de la addenda (por ejemplo <sc:AddendaComercial>), "
                            + "no un cfdi:Comprobante completo");
        }

        // 5. Crear <cfdi:Addenda> con el namespace y prefijo del propio documento
        Element addendaNode = docCfdi.createElementNS(cfdiNamespace, nombreAddenda);
        Node importado = docCfdi.importNode(contenido, true);
        addendaNode.appendChild(importado);

        // 6. Agregar como último hijo (después de cfdi:Complemento, como exige el XSD)
        root.appendChild(addendaNode);

        // 7. Serializar a String en UTF-8, sin indentar para no alterar nodos sellados
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(docCfdi), new StreamResult(writer));
        return writer.toString();
    }

    private static DocumentBuilder crearBuilderSeguro() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Obligatorio para respetar los prefijos cfdi:, tfd:, xsi:, etc.
        factory.setNamespaceAware(true);
        // Protección contra XXE: un CFDI no necesita DTD
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    private static Node buscarHijo(Element padre, String namespace, String nombreLocal) {
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE
                    && nombreLocal.equals(n.getLocalName())
                    && namespace.equals(n.getNamespaceURI())) {
                return n;
            }
        }
        return null;
    }
}