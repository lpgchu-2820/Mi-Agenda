# Agenda Técnico - Reparación de Máquinas de Coser

Aplicación Android base para llevar la agenda de un técnico reparador.

## Funciones incluidas

- Alta y listado de clientes.
- Datos del cliente: nombre/razón social, teléfono, email, domicilio, ciudad y notas.
- Varias máquinas por cliente.
- Datos de máquina: marca, modelo, número de serie, tipo, condición y observaciones.
- Historial de reparaciones por máquina.
- Fecha, descripción del trabajo, diagnóstico, repuestos, mano de obra, total y estado.
- Varias fotografías asociadas a cada reparación.
- Cámara del teléfono integrada.
- Base de datos local Room; los datos quedan en el teléfono.
- Botón «Compartir por WhatsApp» desde el detalle de cada reparación, enviando un resumen del trabajo, diagnóstico, repuestos, mano de obra, total y estado.

## Cómo abrir

1. Descomprimir el ZIP.
2. Abrir la carpeta `AgendaTecnicoMaquinasCoser` desde Android Studio.
3. Esperar el sincronizado de Gradle.
4. Conectar un teléfono Android o crear un emulador.
5. Ejecutar con Run ▶.

## Manifest

Ya está incluido el permiso de cámara:

`<uses-permission android:name="android.permission.CAMERA" />`

También está configurado FileProvider para guardar las fotos de reparaciones.

## Próximas mejoras recomendadas

- Buscar clientes, marcas, modelos y números de serie.
- Editar y eliminar registros.
- Estado: recibido / en reparación / listo / entregado.
- Presupuestos y comprobantes en PDF.
- Firma del cliente.
- Compartir el informe por WhatsApp.
- Copias de seguridad y restauración.
- Sincronización en la nube.
- Catálogo de marcas y modelos.
- Agenda de turnos/calendario.
- Control de repuestos y stock.
- Historial general por cliente.
