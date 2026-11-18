package com.ferbo.consumer;

import java.io.InputStream;

import com.ferbo.cfdi.exporters.CFDIv40JR;
import com.ferbo.consumer.tools.IOTools;

public class AppExample {

	public static void main(String[] args) {
		InputStream xmlStream;
		CFDIv40JR cfdiJR;
		byte[] pdfFile;

		try {
			//xmlStream = IOTools.readFile("/home/user/Descargas/CPD_1.xml");
			//xmlStream = IOTools.readFile("/home/user/Descargas/CDP_2.xml");
			xmlStream = IOTools.readFile("/home/user/Descargas/CPD_3.xml");
			//xmlStream = IOTools.readFile("/home/user/Descargas/CPD_4.xml");
			//xmlStream = IOTools.readFile("/home/user/Descargas/Factura_1.xml");
			//xmlStream = IOTools.readFile("/home/user/Descargas/Factura_2.xml");
			//xmlStream = IOTools.readFile("/home/user/Descargas/Factura_3.xml");
			//xmlStream = IOTools.readFile("/home/user/Descargas/Factura_4.xml");
			//xmlStream = IOTools.readFile("/home/user/Descargas/Factura_5.xml");

			cfdiJR = new CFDIv40JR();
			cfdiJR.setObservacion("Observación de prueba");
			cfdiJR.setLogo("/home/user/github/cfdi-consumer/src/main/resources/images/logo.png");
			//cfdiJR.setLogo("/home/user/Descargas/image-x-generic-symbolic-svgrepo-com.png");
			pdfFile = cfdiJR.generarFormatoImpreso(xmlStream);
			// El arreglo de bytes está listo para responderse en un HTTP Response,
			// enviarse por correo, o guardarse en un FileSystem/S3

			IOTools.writeFile("/home/user/Descargas/Factura.pdf", pdfFile);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
