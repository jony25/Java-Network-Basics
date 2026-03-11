# Sistema de Comunicación Híbrido (TCP/UDP) en Java

Este proyecto implementa una arquitectura cliente-servidor para la transmisión de datos en tiempo real, diferenciando entre el plano de control (señalización y texto) y el plano de usuario (transmisión de voz).

## Fundamentos Técnicos

El sistema se basa en el modelo de referencia OSI, operando en la **Capa de Transporte (Capa 4)** mediante el uso de sockets de red.

### 1. Canal de Texto y Control (TCP)
Se utiliza el protocolo **TCP (Transmission Control Protocol)** para la mensajería y la señalización de comandos de estado.
- **Fiabilidad:** Se garantiza la entrega de paquetes en orden mediante el mecanismo de acuse de recibo (ACK).
- **Arquitectura:** Gestión multihilo mediante *Thread-per-client* con colecciones sincronizadas para evitar condiciones de carrera en el acceso a descriptores de salida.



### 2. Canal de Voz / VoIP (UDP)
Para el flujo de audio se emplea **UDP (User Datagram Protocol)**, priorizando la reducción de la latencia sobre la fiabilidad de entrega.
- **Procesado de Señal:**
  - **Frecuencia de muestreo ($f_s$):** $8000$ Hz. Según el Teorema de Nyquist-Shannon, permite reconstruir señales con un ancho de banda de hasta $4$ kHz.
  - **Cuantificación:** $16$ bits por muestra (PCM lineal).
  - **Tasa de bits (Bitrate):** $$8000 \text{ samples/s} \times 16 \text{ bits/sample} = 128 \text{ kbps}$$



## Estructura del Software

- **`ChatServer.java`**: Proceso central que gestiona la escucha en el puerto $12345$ y la difusión de tramas de texto.
- **`ChatClient.java`**: Terminal de usuario que integra el manejo de sockets TCP y la instanciación de hilos de audio bajo demanda.
- **`VoiceSender.java` / `VoiceReceiver.java`**: Módulos encargados de la captura/reproducción de audio mediante la API `javax.sound.sampled` y el encapsulamiento en datagramas UDP.

## Configuración y Ejecución

1. Compilar el código fuente:
   ```bash
   javac *.java