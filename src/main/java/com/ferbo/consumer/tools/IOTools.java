package com.ferbo.consumer.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IOTools {

	/**
	 * • Lee un archivo desde el sistema de archivos y lo devuelve como un
	 * InputStream previo control de la ruta. •
	 * 
	 * @param pathToFile Ruta absoluta o relativa del archivo.
	 * @return InputStream listo para ser procesado.
	 * @throws FileNotFoundException    Si la ruta no existe.
	 * @throws IllegalArgumentException Si la ruta apunta a un directorio en lugar de un archivo.
	 */
	public static synchronized InputStream readFile(String pathToFile) throws FileNotFoundException {
		if (pathToFile == null || pathToFile.trim().isEmpty())
			throw new IllegalArgumentException("La ruta del archivo no puede estar vacía o ser nula.");
		
		File file = new File(pathToFile);
		if (!file.exists())
			throw new FileNotFoundException("El archivo no existe en la ruta especificada: " + pathToFile);
		
		if (file.isDirectory())
			throw new IllegalArgumentException("La ruta proporcionada corresponde a un directorio, debe ser un archivo: " + pathToFile);
		
		return new FileInputStream(file);
	}

	/**
     * Guarda un arreglo de bytes en un archivo en el sistema de archivos.
     * 
     * @param fullPath El nombre del archivo con su ruta absoluta o relativa.
     * @param data El arreglo de bytes a escribir.
     * @throws IOException Si ocurre un error de entrada/salida al escribir el archivo.
     */
    public static void writeFile(String fullPath, byte[] data) throws IOException {
        if (fullPath == null || data == null) {
            throw new IllegalArgumentException("La ruta y los datos no pueden ser nulos.");
        }
        
        // Files.write se encarga de abrir, escribir y cerrar el archivo automáticamente
        Files.write(Paths.get(fullPath), data);
    }
}
