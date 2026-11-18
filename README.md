# cfdi-consumer

Proyecto de **pruebas** que consume la librería [`cfdi40-printer`](https://github.com/TU_USUARIO_U_ORG/cfdi40-printer) para generar la representación impresa (PDF) de un CFDI 4.0 a partir de su XML. No es una librería ni se publica: sirve para validar manualmente el comportamiento de `cfdi40-model` y `cfdi40-printer` con XML reales.

## Contenido

| Clase | Descripción |
|---|---|
| `IOTools` | Utilidades para leer y escribir archivos en el sistema de archivos (`readFile`, `writeFile`). |
| `AddendaInjector` | Agrega un nodo `cfdi:Addenda` a un CFDI ya timbrado (por el PAC o el SAT), con el contenido que solicite un cliente. |
| `AppExample` | Aplicación de ejemplo con método `main`. Lee un XML, invoca a `cfdi40-printer` y guarda el PDF resultante. |

## Requisitos

- JDK 8
- Apache Maven 3.x
- `cfdi40-model` y `cfdi40-printer` disponibles en tu repositorio local de Maven (`mvn clean install` en cada proyecto) o en GitHub Packages.

## Dependencias

```xml
<dependency>
  <groupId>com.my.app.sat</groupId>
  <artifactId>cfdi40-printer</artifactId>
  <version>x.y.z</version>
</dependency>
```

`cfdi40-model` llega de forma transitiva a través de `cfdi40-printer`. Sustituye `x.y.z` por la versión publicada.

> Si las dependencias se descargan de GitHub Packages, configura en `~/.m2/settings.xml` el `<server>` con el mismo `id` del repositorio y un token con permiso `read:packages`.

## Uso

El flujo de `AppExample` es el siguiente:

1. Leer el XML del CFDI como `InputStream`.
2. (Opcional) Agregar una Addenda con `AddendaInjector`.
3. Crear una instancia de `CFDIv40JR` y configurarla (observación y logo).
4. Invocar `generarFormatoImpreso(xmlStream)`, que devuelve el PDF como `byte[]`.
5. Usar el arreglo de bytes: guardarlo en disco, responderlo en una respuesta HTTP, enviarlo por correo o subirlo a S3.

```java
InputStream xmlStream = IOTools.readFile("/ruta/al/comprobante.xml");

CFDIv40JR cfdiJR = new CFDIv40JR();
cfdiJR.setObservacion("Observación de prueba");
cfdiJR.setLogo("/ruta/al/logo.png");

byte[] pdfFile = cfdiJR.generarFormatoImpreso(xmlStream);

IOTools.writeFile("/ruta/de/salida/Factura.pdf", pdfFile);
```

### Parámetros configurables

| Método | Descripción |
|---|---|
| `setObservacion(String)` | Texto libre que se muestra como observación en el reporte. |
| `setLogo(String)` | Ruta de la imagen (por ejemplo, PNG) que se usa como logo del emisor. |

## Addenda (`AddendaInjector`)

La **Addenda** es un nodo opcional del CFDI (`cfdi:Addenda`) donde un cliente puede solicitar información comercial adicional: orden de compra, número de proveedor, sucursal de entrega, etc. `AddendaInjector` la inserta en un XML que ya viene timbrado.

```java
public static String agregarAddenda(String xmlCfdiTimbrado, String xmlContenidoAddenda) throws Exception
```

| Parámetro | Descripción |
|---|---|
| `xmlCfdiTimbrado` | XML completo del CFDI 4.0 ya timbrado, como `String`. |
| `xmlContenidoAddenda` | XML del **contenido** de la addenda (solo el nodo raíz del cliente, por ejemplo `<sc:AddendaComercial>`), con sus namespaces declarados. **No** es un CFDI completo. |
| Retorno | El CFDI con el nodo `<cfdi:Addenda>` agregado, en UTF-8. |

### Cómo funciona

1. Parsea el CFDI con soporte de namespaces, para respetar los prefijos `cfdi:`, `tfd:`, `xsi:`, etc.
2. Crea el nodo `cfdi:Addenda` con el namespace del CFDI 4.0.
3. Parsea el contenido de la addenda e importa su nodo raíz dentro de `cfdi:Addenda`.
4. Agrega `cfdi:Addenda` como último hijo de `cfdi:Comprobante`, después de `cfdi:Complemento`.
5. Serializa el documento a `String` en UTF-8, **sin indentar**, para no alterar los espacios del XML.

### ¿Por qué se puede agregar después del timbrado?

El sello del emisor (`Sello`) y el del SAT (`SelloSAT`) se calculan sobre la *cadena original*, que **no incluye** el nodo `Addenda`. Por eso agregarla después del timbrado no invalida el comprobante.

### Ejemplo de contenido de addenda

Este es el valor que se pasa en `xmlContenidoAddenda`:

```xml
<sc:AddendaComercial xmlns:sc="http://supermercadoscentro.com.mx" version="1.0">
  <sc:DatosGenerales>
    <sc:OrdenCompra>OC-2026-88941</sc:OrdenCompra>
    <sc:NumeroProveedor>PROV-00452</sc:NumeroProveedor>
    <sc:SucursalEntrega codigo="SUC-IZT-04" nombre="Bodega Central Iztapalapa"/>
    <sc:CitaRecibo folio="CITA-9920" fecha="2026-11-18"/>
  </sc:DatosGenerales>
  <sc:DetalleArticulos>
    <sc:Articulo numeroLinea="1" skuCliente="7501000123456" codigoProveedor="CAJA-ACEITE-12" cantidadFacturada="10" precioPactado="100.00"/>
  </sc:DetalleArticulos>
</sc:AddendaComercial>
```

Resultado: el CFDI original, con la addenda al final.

```xml
<cfdi:Comprobante ...>
  <cfdi:Emisor .../>
  <cfdi:Receptor .../>
  <cfdi:Conceptos>...</cfdi:Conceptos>
  <cfdi:Impuestos>...</cfdi:Impuestos>
  <cfdi:Complemento>
    <tfd:TimbreFiscalDigital .../>
  </cfdi:Complemento>
  <cfdi:Addenda>
    <sc:AddendaComercial xmlns:sc="http://supermercadoscentro.com.mx" version="1.0">
      ...
    </sc:AddendaComercial>
  </cfdi:Addenda>
</cfdi:Comprobante>
```

### Uso junto con el generador de PDF

```java
String xmlTimbrado = new String(Files.readAllBytes(Paths.get("/ruta/al/comprobante.xml")), StandardCharsets.UTF_8);
String addenda = new String(Files.readAllBytes(Paths.get("/ruta/a/addenda.xml")), StandardCharsets.UTF_8);

String xmlConAddenda = AddendaInjector.agregarAddenda(xmlTimbrado, addenda);

InputStream xmlStream = new ByteArrayInputStream(xmlConAddenda.getBytes(StandardCharsets.UTF_8));
byte[] pdfFile = new CFDIv40JR().generarFormatoImpreso(xmlStream);
```

### Consideraciones

- El XML con addenda es el que se **entrega al cliente**. El timbrado y la cancelación ante el SAT se refieren al comprobante sin addenda, por lo que conviene conservar también el XML original.
- Si el CFDI ya trae un `cfdi:Addenda`, la clase no lo detecta y agregaría un segundo nodo, lo que es inválido (el esquema permite solo uno). Valida antes de invocarla.
- La clase no valida el contenido de la addenda contra el XSD del cliente.
- Si el XML de la addenda no declara sus namespaces, la serialización puede producir prefijos sin declarar.
- Usa siempre `UTF-8` al convertir entre `String` y `byte[]`, para no corromper acentos ni la `ñ`.

## Ejecución

Antes de ejecutar, edita en `AppExample` las rutas del XML de entrada, del logo y del PDF de salida. Después:

```bash
mvn clean compile exec:java -Dexec.mainClass=com.my.app.cfdi.AppExample
```

> Ajusta el nombre del paquete de `AppExample` al real. Si el `pom.xml` no incluye `exec-maven-plugin`, puedes ejecutar la clase desde tu IDE.

El PDF se genera en la ruta indicada en `IOTools.writeFile(...)`.

## Casos de prueba

`AppExample` incluye líneas comentadas para cambiar rápidamente de comprobante. Descomenta solo una a la vez:

| Archivo | Tipo de comprobante |
|---|---|
| `CPD_1.xml` ... `CPD_4.xml` | Comprobantes con complemento de pago (Pagos 2.0) |
| `Factura_1.xml` ... `Factura_5.xml` | Facturas (ingreso) |

> Los XML de prueba **no se incluyen en el repositorio**: pueden contener datos fiscales reales (RFC, importes, UUID). Usa comprobantes de prueba o anonimizados y no los subas a git.

## Notas

- Las rutas del ejemplo son absolutas y específicas de un equipo. Para uso compartido, conviene recibirlas como argumentos (`args`) o leerlas de variables de entorno.
- El bloque `catch` solo imprime la traza (`printStackTrace`). Es suficiente para pruebas; en una aplicación real usa un logger y maneja el error según el contexto.
- Cierra el `InputStream` después de usarlo (por ejemplo, con `try-with-resources`) para no dejar archivos abiertos.
- Este proyecto no se publica ni se versiona como artefacto; solo se usa para pruebas manuales.

## Solución de problemas

| Síntoma | Causa probable |
|---|---|
| `NoSuchFileException` o `FileNotFoundException` | La ruta del XML, del logo o de salida no existe o no tiene permisos. |
| Maven no resuelve `cfdi40-printer` | No se ejecutó `mvn clean install` en el proyecto, o falta el `settings.xml` para GitHub Packages. |
| El PDF sale sin logo | La ruta de `setLogo` es incorrecta o la imagen no es legible. |
| Error de parseo del XML | El archivo no es un CFDI 4.0 válido o contiene un complemento no soportado por `cfdi40-model`. |
| `SAXParseException` en `AddendaInjector` | `xmlContenidoAddenda` no es un XML bien formado, o se pasó un CFDI completo en lugar del nodo de la addenda. |
| La addenda queda fuera de lugar o el PAC/cliente rechaza el XML | El namespace usado para `cfdi:Addenda` no es el del CFDI 4.0, o la addenda quedó duplicada. |