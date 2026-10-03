package com.juanp.custommobs.docs;

import com.juanp.custommobs.CustomMobsPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Copia la documentacion y los tutoriales que viajan dentro del jar a la carpeta del
 * plugin, para que un admin pueda leerlos sin abrir el jar.
 *
 * <p><b>No sobrescribe</b>: si el archivo ya existe —porque el admin lo edito o lo
 * tradujo— se respeta. Solo escribe lo que falta.
 */
public final class DocsInstaller {

    /** Carpetas del jar que se copian a la carpeta del plugin. */
    private static final List<String> FOLDERS = List.of("docs", "tutorials");

    private final CustomMobsPlugin plugin;

    public DocsInstaller(CustomMobsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Copia lo que falte. Devuelve cuantos archivos escribio. */
    public int install() {
        File jar = this.plugin.jarFile();
        if (jar == null) {
            return 0;
        }
        if (jar.isDirectory()) {
            // Corriendo desde clases sueltas (un IDE): se copia desde el arbol de archivos.
            return this.copyFromFolder(jar.toPath());
        }
        return this.copyFromJar(jar);
    }

    private int copyFromJar(File jar) {
        int copied = 0;
        try (JarFile archive = new JarFile(jar)) {
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !this.isDoc(entry.getName())) {
                    continue;
                }
                File target = new File(this.plugin.getDataFolder(), entry.getName());
                if (target.exists() || !this.ensureParent(target)) {
                    continue;
                }
                try (InputStream in = archive.getInputStream(entry)) {
                    Files.copy(in, target.toPath());
                    copied++;
                }
            }
        } catch (IOException ex) {
            this.plugin.getLogger().warning("No se pudo copiar la documentacion: " + ex.getMessage());
        }
        return copied;
    }

    private int copyFromFolder(Path root) {
        int copied = 0;
        for (String folder : FOLDERS) {
            Path source = root.resolve(folder);
            if (!Files.isDirectory(source)) {
                continue;
            }
            try (var walk = Files.walk(source)) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    String relative = root.relativize(file).toString().replace(File.separatorChar, '/');
                    if (!this.isDoc(relative)) {
                        continue;
                    }
                    File target = new File(this.plugin.getDataFolder(), relative);
                    if (target.exists() || !this.ensureParent(target)) {
                        continue;
                    }
                    Files.copy(file, target.toPath());
                    copied++;
                }
            } catch (IOException ex) {
                this.plugin.getLogger().warning("No se pudo copiar la documentacion: " + ex.getMessage());
            }
        }
        return copied;
    }

    private boolean ensureParent(File target) {
        File parent = target.getParentFile();
        return parent == null || parent.isDirectory() || parent.mkdirs();
    }

    /** {@code true} si esa ruta del jar es documentacion que hay que copiar. */
    private boolean isDoc(String name) {
        for (String folder : FOLDERS) {
            if (name.startsWith(folder + "/") && (name.endsWith(".md") || name.endsWith(".txt"))) {
                return true;
            }
        }
        return false;
    }
}
