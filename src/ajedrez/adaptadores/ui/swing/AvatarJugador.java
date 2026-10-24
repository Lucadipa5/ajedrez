package ajedrez.adaptadores.ui.swing;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Avatar redondo del jugador: su foto de perfil (o un "?" si no tiene) con anillo dorado,
 * como los íconos de invocador, y una medalla con el nivel.
 * También tiene las utilidades para convertir una imagen elegida en la foto que se guarda.
 */
class AvatarJugador extends JComponent {

    /** Lado en píxeles con el que se guarda la foto (cuadrada). */
    static final int LADO_FOTO = 256;

    private BufferedImage foto;
    private Integer nivel;
    private BufferedImage fotoEscalada;

    AvatarJugador(int tamano) {
        setPreferredSize(new Dimension(tamano, tamano));
        setMinimumSize(new Dimension(tamano, tamano));
        setMaximumSize(new Dimension(tamano, tamano));
    }

    void setDatos(byte[] fotoCodificada, Integer nivel) {
        this.foto = decodificar(fotoCodificada);
        this.nivel = nivel;
        this.fotoEscalada = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        int d = Math.min(getWidth(), getHeight()) - 4;
        int x = (getWidth() - d) / 2, y = 2;
        Ellipse2D.Double circulo = new Ellipse2D.Double(x, y, d, d);

        if (foto != null) {
            int lado = d * 2; // un poco más grande que el círculo para que se vea nítida
            if (fotoEscalada == null || fotoEscalada.getWidth() != lado) {
                fotoEscalada = Tema.escalarConCalidad(foto, lado, lado, true);
            }
            Shape clipAnterior = g2.getClip();
            g2.clip(circulo);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(fotoEscalada, x, y, d, d, null);
            g2.setClip(clipAnterior);
        } else {
            g2.setPaint(new RadialGradientPaint(x + d / 2f, y + d / 3f, d * 0.7f, new float[]{0f, 1f},
                    new Color[]{new Color(30, 50, 70), new Color(8, 14, 22)}));
            g2.fill(circulo);
            String texto = nivel == null ? "?" : String.valueOf(nivel);
            g2.setFont(Tema.titulo(d * (texto.length() > 1 ? 0.36f : 0.44f)));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Tema.TEXTO);
            g2.drawString(texto, x + (d - fm.stringWidth(texto)) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());
        }

        // Anillo dorado + línea interior hextech
        g2.setStroke(new BasicStroke(Math.max(2f, d / 22f)));
        g2.setPaint(new GradientPaint(0, y, Tema.DORADO_BRILLO, 0, y + d, Tema.DORADO_OSCURO));
        g2.draw(circulo);
        g2.setColor(new Color(10, 200, 185, 90));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Ellipse2D.Double(x + 4, y + 4, d - 8, d - 8));

        // Con foto, el nivel va en una medalla abajo
        if (foto != null && nivel != null) {
            String texto = String.valueOf(nivel);
            g2.setFont(Tema.titulo(Math.max(9f, d * 0.2f)));
            FontMetrics fm = g2.getFontMetrics();
            int mw = Math.max(fm.stringWidth(texto) + 10, (int) (d * 0.34)), mh = fm.getHeight();
            int mx = x + (d - mw) / 2, my = y + d - mh / 2 - 2;
            Shape medalla = Tema.octogono(mx, my, mw, mh, 4);
            g2.setColor(Tema.FONDO_OSCURO);
            g2.fill(medalla);
            g2.setColor(Tema.DORADO);
            g2.draw(medalla);
            g2.setColor(Tema.TEXTO);
            g2.drawString(texto, mx + (mw - fm.stringWidth(texto)) / 2, my + (mh - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }

    // -----------------------------------------------------------------------
    // Procesamiento de la foto
    // -----------------------------------------------------------------------

    /**
     * Lee la imagen elegida, la recorta al cuadrado central y la achica a {@link #LADO_FOTO}.
     * @return la foto codificada en PNG, lista para guardar en el perfil
     * @throws IOException si el archivo no es una imagen que se pueda leer
     */
    static byte[] prepararFoto(File archivo) throws IOException {
        BufferedImage original = ImageIO.read(archivo);
        if (original == null) {
            throw new IOException("El archivo no es una imagen compatible (usá PNG, JPG, GIF o BMP).");
        }
        int lado = Math.min(original.getWidth(), original.getHeight());
        BufferedImage cuadrada = original.getSubimage(
                (original.getWidth() - lado) / 2, (original.getHeight() - lado) / 2, lado, lado);
        BufferedImage chica = Tema.escalarConCalidad(cuadrada, LADO_FOTO, LADO_FOTO, true);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(chica, "png", salida);
        return salida.toByteArray();
    }

    static BufferedImage decodificar(byte[] fotoCodificada) {
        if (fotoCodificada == null) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(fotoCodificada));
        } catch (IOException e) {
            return null;
        }
    }
}
