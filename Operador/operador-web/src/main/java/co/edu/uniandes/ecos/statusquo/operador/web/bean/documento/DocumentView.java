package co.edu.uniandes.ecos.statusquo.operador.web.bean.documento;

import co.edu.uniandes.ecos.statusquo.operador.ejb.CompartirEJB;
import co.edu.uniandes.ecos.statusquo.operador.ejb.DocumentoEJB;
import co.edu.uniandes.ecos.statusquo.operador.ejb.PropertiesEJB;
import co.edu.uniandes.ecos.statusquo.operador.ejb.UsuarioEJB;
import co.edu.uniandes.ecos.statusquo.operador.entity.Archivo;
import co.edu.uniandes.ecos.statusquo.operador.entity.Carpeta;
import co.edu.uniandes.ecos.statusquo.operador.entity.EstadoArchivo;
import co.edu.uniandes.ecos.statusquo.operador.entity.FormatoArchivo;
import co.edu.uniandes.ecos.statusquo.operador.entity.Usuario;
import co.edu.uniandes.ecos.statusquo.operador.web.bean.UtilBean;
import co.edu.uniandes.ecos.statusquo.operador.web.util.TreeNodeHelper;
import co.edu.uniandes.ecos.statusquo.operador.ws.dto.UsuarioDTO;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.TreeDragDropEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;

@ManagedBean(name = "documentView")
@ViewScoped
public class DocumentView implements Serializable {

    private TreeNode root;

    private List<Carpeta> carpetasUsuario;

    private List<Archivo> archivosUsuario;

    private Carpeta carpetaSeleccionada;

    private Archivo selectedDocument;

    private String nombreCarpetaNueva;

    private StreamedContent contenidoDescarga;

    private Archivo nuevoDocumento;

    // Compartir
    private String compartirTo;

    private String entidadPublica;

    private boolean entidadPublicaRendered;

    private String identificacionPersona;

    private String nombrePersona;

    private UsuarioDTO personaBuscada;

    @EJB
    private DocumentoEJB documentoEJB;

    @EJB
    private PropertiesEJB propertiesEJB;

    @EJB
    private UsuarioEJB usuarioEJB;

    @EJB
    private CompartirEJB compartirEJB;

    @PostConstruct
    public void init() {
        Usuario usuario = UtilBean.getUsuarioActual();
        carpetasUsuario = documentoEJB.traerCarpetasDeUsuario(usuario);
        if (carpetasUsuario != null && !carpetasUsuario.isEmpty()) {
            root = TreeNodeHelper.toTreeNode(carpetasUsuario);
            if (root != null && root.getChildren() != null && !root.getChildren().isEmpty()) {
                TreeNode primera = root.getChildren().get(0);
                primera.setSelected(true);
                primera.setExpanded(true);
                carpetaSeleccionada = (Carpeta) primera.getData();
                archivosUsuario = documentoEJB.traerArchivosCarpeta(carpetaSeleccionada, usuario);
            }
        }
    }

    public TreeNode getRoot() {
        return root;
    }

    public Archivo getSelectedDocument() {
        return selectedDocument;
    }

    public void setSelectedDocument(Archivo selectedDocument) {

        this.selectedDocument = selectedDocument;
    }

    public List<Archivo> getArchivosUsuario() {
        return archivosUsuario;
    }

    public void setArchivosUsuario(List<Archivo> archivosUsuario) {
        this.archivosUsuario = archivosUsuario;
    }

    public Archivo getNuevoDocumento() {
        return nuevoDocumento;
    }

    public void setNuevoDocumento(Archivo nuevoDocumento) {
        this.nuevoDocumento = nuevoDocumento;
    }

    public StreamedContent getContenidoDescarga() throws Exception {
        System.out.println("archivo a descargar");
        try {
            if (selectedDocument.isRemoto()) {
                contenidoDescarga = new DefaultStreamedContent(documentoEJB.getArchivoRemoto(selectedDocument.getIdentificacionPropietario(), selectedDocument.getUrl()), null, selectedDocument.getNombre() + "." + selectedDocument.getFormato().getExtencion());
            } else {
                InputStream is = new FileInputStream(selectedDocument.getUrl());
                contenidoDescarga = new DefaultStreamedContent(is, null, selectedDocument.getNombre() + "." + selectedDocument.getFormato().getExtencion());
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(DocumentView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return contenidoDescarga;
    }

    public Carpeta getCarpetaSeleccionada() {
        return carpetaSeleccionada;
    }

    public String getNombreCarpetaNueva() {
        return nombreCarpetaNueva;
    }

    public void setNombreCarpetaNueva(String nombreCarpetaNueva) {
        this.nombreCarpetaNueva = nombreCarpetaNueva;
    }

    /**
     * Retorna la cadena con el mensaje de confirmación de eliminación de un
     * archivo
     *
     * @return
     */
    public String getMensajeEliminacion() {
        String mensaje = "¿Está seguro de mover el archivo a la papelera?";
        if (carpetaSeleccionada != null) {
            if (carpetaSeleccionada.getTipo().getId() == 3) {
                mensaje = "¿Está seguro de eliminar el archivo definitivamente?";
            }
        }
        return mensaje;
    }

    /* Métodos basados en eventos */
    /**
     * Método invocado cuando se selecciona una carpeta
     *
     * @param event
     */
    public void onNodeSelect(NodeSelectEvent event) {
        Usuario usuario = UtilBean.getUsuarioActual();
        carpetaSeleccionada = (Carpeta) event.getTreeNode().getData();
        archivosUsuario = documentoEJB.traerArchivosCarpeta(carpetaSeleccionada, usuario);
        selectedDocument = null;
    }

    public void onNodeExpand(NodeExpandEvent event) {
        carpetaSeleccionada = (Carpeta) event.getTreeNode().getData();
        System.out.println("Expandida: " + carpetaSeleccionada.getNombre());
    }
    
    public void onTreeDragDrop(TreeDragDropEvent event) {
        TreeNode dragNode = event.getDragNode();
        TreeNode dropNode = event.getDropNode();
        Carpeta origen = (Carpeta) dragNode.getData();
        Carpeta destino = (Carpeta) dropNode.getData();
        
        System.out.println("Desde: " + origen.getNombre() + " hasta: " + destino.getNombre());
        if (origen.getPrincipal()) {
            FacesMessage message = new FacesMessage();
            message.setSummary("Error");
            message.setDetail("No puede mover esta carpeta");
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            FacesContext.getCurrentInstance().addMessage(null, message);
        } else {
            documentoEJB.moverCarpeta(origen, destino);
        }
        
        root = TreeNodeHelper.toTreeNode(carpetasUsuario);
        
    }

    /**
     * Método invocado para subir un archivo
     *
     * @param event
     */
    public void handleFileUpload(FileUploadEvent event) {
        FacesMessage message = new FacesMessage();
        try {
            Usuario usuario = UtilBean.getUsuarioActual();
            EstadoArchivo estado = new EstadoArchivo(1L); //Activo

            Archivo archivo = new Archivo();
            String nombre = event.getFile().getFileName();

            // Nombre y Extencion a partir del Nombre del Archivo
            int punto = nombre.lastIndexOf(".");

            if (punto >= 0 && punto + 1 < nombre.length()) {
                String extencion = nombre.substring(punto + 1);
                FormatoArchivo formato = documentoEJB.obtenerFormato(extencion);
                if (formato == null) {
                    formato = new FormatoArchivo();
                    formato.setExtencion(extencion);
                    formato.setNombre(extencion.toUpperCase());
                    documentoEJB.crearFormato(formato);
                }
                archivo.setFormato(formato);
                archivo.setNombre(nombre.substring(0, punto));
            }

            archivo.setSizeArchivo(event.getFile().getSize());
            archivo.setFecha(new Date());
            String url = propertiesEJB.getProperty("almacenamiento.ruta")
                    + "/" + usuario.getDocumento()
                    + "/" + archivo.getNombre();
            archivo.setUrl(url);
            archivo.setEstadoId(estado);
            archivo.setCarpetaPadreId(carpetaSeleccionada);
            archivo.setCarpetaPersonal(usuario.getCarpetaPersonal());
            archivo.setContenido(event.getFile().getContents());

            archivo.setFirmado(false);//Por defecto no sube firmado
//            archivo.setIdentificacionPropietario(usuario.getDocumento());
            archivo.setTipo(documentoEJB.getTipoArchivoGenerico());

            documentoEJB.crearArchivo(archivo);

            carpetaSeleccionada.getArchivos().add(archivo);
            archivosUsuario.add(archivo);

            archivosUsuario = documentoEJB.traerArchivosCarpeta(carpetaSeleccionada, usuario);
            selectedDocument = null;

            //Mensaje en JSF
            message.setSummary("Carga exitosa");
            message.setDetail(event.getFile().getFileName() + " cargado con éxito.");
            message.setSeverity(FacesMessage.SEVERITY_INFO);

        } catch (Exception e) {
            message.setSummary("Error");
            message.setDetail(event.getFile().getFileName() + " no pudo ser cargado");
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
        }
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void borrarArchivo() {
        archivosUsuario.remove(selectedDocument);
        documentoEJB.borrarArchivo(selectedDocument);
        selectedDocument = null;
    }

    public void restaurarArchivo() {
        archivosUsuario.remove(selectedDocument);
        documentoEJB.restaurarArchivo(selectedDocument);
        selectedDocument = null;
    }

    public void crearCarpeta() {
        documentoEJB.crearCarpeta(carpetaSeleccionada, nombreCarpetaNueva);
        nombreCarpetaNueva = "";
        root = TreeNodeHelper.toTreeNode(carpetasUsuario);
    }

    public void borrarCarpeta() {
        documentoEJB.borrarCarpeta(carpetaSeleccionada);
        Carpeta padre = carpetaSeleccionada.getCarpetaPadre();
        padre.getCarpetasHijas().remove(carpetaSeleccionada);
        root = TreeNodeHelper.toTreeNode(carpetasUsuario);
    }

    public String obtenerSizeKB(final Long size) {
        String rsp = null;
        if (size != null) {
            if (size < 1024l) {
                rsp = size + " bytes";
            } else if (size < 1048576l) {
                rsp = (size / 1024l) + " KB";
            } else {
                rsp = (size / 1048576l) + " MB";
            }
        }
        return rsp;
    }

    // Metodos para compartir
    public void nuevoCompartir() {
        compartirTo = null;
        entidadPublica = null;
        entidadPublicaRendered = false;
        identificacionPersona = null;
        nombrePersona = null;
    }

    public void seleccionarCompartirTo() {
        entidadPublicaRendered = compartirTo != null && compartirTo.equals("EntidadPublica");
        identificacionPersona = null;
        entidadPublica = null;
    }

    public void buscarPersona() throws Exception {
        System.out.println("Buscar Persona");
        personaBuscada = usuarioEJB.buscarUsuario(identificacionPersona);
        if (personaBuscada == null) {
            nombrePersona = null;
        } else {
            nombrePersona = personaBuscada.getNombreCompleto();
        }
    }

    public void compartirArchivo() throws Exception {
        System.out.println("Compartir");
        if (isCompartirRendered()) {
            UsuarioDTO usuarioCompartir;
            if (personaBuscada != null) {
                usuarioCompartir = personaBuscada;
            } else {
                usuarioCompartir = usuarioEJB.buscarUsuario(entidadPublica);
            }
            Usuario usuario = UtilBean.getUsuarioActual();
            compartirEJB.compartirArchivo(usuario, usuarioCompartir, selectedDocument);
        }
    }

    public boolean isCompartirRendered() {
        return personaBuscada != null || (entidadPublica != null && !entidadPublica.equals("---"));
    }

    public String getCompartirTo() {
        return compartirTo;
    }

    public void setCompartirTo(String compartirTo) {
        this.compartirTo = compartirTo;
    }

    public String getEntidadPublica() {
        return entidadPublica;
    }

    public void setEntidadPublica(String entidadPublica) {
        this.entidadPublica = entidadPublica;
    }

    public boolean isEntidadPublicaRendered() {
        return entidadPublicaRendered;
    }

    public void setEntidadPublicaRendered(boolean entidadPublicaRendered) {
        this.entidadPublicaRendered = entidadPublicaRendered;
    }

    public String getIdentificacionPersona() {
        return identificacionPersona;
    }

    public void setIdentificacionPersona(String identificacionPersona) {
        this.identificacionPersona = identificacionPersona;
    }

    public String getNombrePersona() {
        return nombrePersona;
    }

}
