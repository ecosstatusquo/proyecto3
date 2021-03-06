package co.edu.uniandes.ecos.statusquo.operador.ejb;

import co.edu.uniandes.ecos.statusquo.operador.dao.ArchivoDAO;
import co.edu.uniandes.ecos.statusquo.operador.dao.CarpetaDAO;
import co.edu.uniandes.ecos.statusquo.operador.dao.EstadoArchivoDAO;
import co.edu.uniandes.ecos.statusquo.operador.dao.FormatoArchivoDAO;
import co.edu.uniandes.ecos.statusquo.operador.dao.MensajeDAO;
import co.edu.uniandes.ecos.statusquo.operador.dao.PermisoDAO;
import co.edu.uniandes.ecos.statusquo.operador.dao.TipoArchivoDAO;
import co.edu.uniandes.ecos.statusquo.operador.dao.TipoMensajeDAO;
import co.edu.uniandes.ecos.statusquo.operador.entity.Archivo;
import co.edu.uniandes.ecos.statusquo.operador.entity.Carpeta;
import co.edu.uniandes.ecos.statusquo.operador.entity.CarpetaPersonal;
import co.edu.uniandes.ecos.statusquo.operador.entity.FormatoArchivo;
import co.edu.uniandes.ecos.statusquo.operador.entity.Mensaje;
import co.edu.uniandes.ecos.statusquo.operador.entity.Permiso;
import co.edu.uniandes.ecos.statusquo.operador.entity.TipoArchivo;
import co.edu.uniandes.ecos.statusquo.operador.entity.Usuario;
import co.edu.uniandes.ecos.statusquo.operador.ws.dto.UsuarioDTO;
import co.edu.uniandes.ecos.statusquo.operador.ws.imp.NotificacionCompartidoDTO;
import co.edu.uniandes.ecos.statusquo.operador.ws.service.NotificacionService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 *
 * @author Alvaro
 */
@Stateless
@LocalBean
public class CompartirEJB {

    @EJB
    private CarpetaDAO carpetaDAO;

    @EJB
    private ArchivoDAO archivoDAO;

    @EJB
    private FormatoArchivoDAO formatoDAO;

    @EJB
    private PermisoDAO permisoDAO;

    @EJB
    private MensajeDAO mensajeDAO;

    @EJB
    private TipoMensajeDAO tipoMensajeDAO;

    @EJB
    private TipoArchivoDAO tipoArchivoDAO;

    @EJB
    private EstadoArchivoDAO estadoArchivoDAO;

    @EJB
    private UsuarioEJB usuarioEJB;

    @EJB
    private NotificacionService notificacionService;

    public void compartirArchivo(final Usuario usuarioPropietario, final UsuarioDTO usuarioDestinoDTO, final Archivo arhivoLocal) {
        registrarPermisoCompartido(arhivoLocal, usuarioDestinoDTO.getDocumento());

        if (usuarioDestinoDTO.isUsuarioLocal()) {

            Usuario usuarioDestino = usuarioEJB.obtenerUsuario(usuarioDestinoDTO.getDocumento());

            CarpetaPersonal carpetaPersonalDestino = usuarioDestino.getCarpetaPersonal();

            Archivo archivoDestino = registrarArchivoRemoto(carpetaPersonalDestino, arhivoLocal, usuarioPropietario.getDocumento());

            registrarNotificacionCompartidoLocal(carpetaPersonalDestino, usuarioPropietario.getDocumento(), archivoDestino);
        } else {

            NotificacionCompartidoDTO notificacion = new NotificacionCompartidoDTO();
            notificacion.setTextoMensaje(usuarioPropietario.getNombreCompleto() + " le compartió el archivo " + arhivoLocal.getNombre());
            notificacion.setFormato(arhivoLocal.getFormato().getExtencion());
            notificacion.setIdArchivo(arhivoLocal.getId() + "");
            notificacion.setIdentificacionDestinatario(usuarioDestinoDTO.getDocumento());
            notificacion.setIdentificacionRemitente(usuarioPropietario.getDocumento());
            notificacion.setNombreArchivo(arhivoLocal.getNombre());
            notificacion.setNombreTipoArchivo(arhivoLocal.getTipo().getNombre());
            notificacion.setSizeArchivo(arhivoLocal.getSizeArchivo() + "");

            notificacionService.notificarCompartido(notificacion, usuarioDestinoDTO.getOperador().getUrl());
        }
    }

    public Archivo registrarArchivoRemoto(final CarpetaPersonal carpetaPersonal, final Archivo archivoOriginal, final String identificacionPropietario) {

        Carpeta carpeta = carpetaDAO.getCarpetaPrincipal(carpetaPersonal.getId());

        Archivo archivo = new Archivo();
        archivo.setCarpetaPadreId(carpeta);
        archivo.setCarpetaPersonal(carpetaPersonal);
        archivo.setEstadoId(estadoArchivoDAO.getEstadoActivo());
        archivo.setFecha(new Date());
        archivo.setFirmado(Boolean.FALSE);
        archivo.setFormato(archivoOriginal.getFormato());
        archivo.setIdentificacionPropietario(identificacionPropietario);
        archivo.setNombre(archivoOriginal.getNombre());
        archivo.setSizeArchivo(archivoOriginal.getSizeArchivo());
        archivo.setTipo(archivoOriginal.getTipo());
        archivo.setUrl(archivoOriginal.getId() + "");
        archivoDAO.insertar(archivo);
        carpeta.addArchivo(archivo);
        return archivo;
    }

    public void registrarNotificacionCompartidoLocal(final CarpetaPersonal carpetaPersonal, final String documentoRemitente, final Archivo archivoDestino) {

        Mensaje notificacion = new Mensaje();
        notificacion.setTipo(tipoMensajeDAO.getTipoNotificacionCompartido());
        notificacion.setCarpetaPersonal(carpetaPersonal);
        notificacion.setIdentificacionRemitente(documentoRemitente);
        notificacion.setTexto("El usuario " + documentoRemitente + " le compartió el archivo " + archivoDestino.getNombre());
        notificacion.setArchivo(archivoDestino);
        mensajeDAO.insertar(notificacion);
    }

    public void registrarNotificacionCompartidoRemoto(final CarpetaPersonal carpetaPersonal, final String documentoRemitente, final Archivo archivoDestino, final String texto) {

        Mensaje notificacion = new Mensaje();
        notificacion.setTipo(tipoMensajeDAO.getTipoNotificacionCompartido());
        notificacion.setCarpetaPersonal(carpetaPersonal);
        notificacion.setIdentificacionRemitente(documentoRemitente);
        notificacion.setTexto(texto);
        notificacion.setArchivo(archivoDestino);
        mensajeDAO.insertar(notificacion);
    }

    public void registrarPermisoCompartido(final Archivo archivo, final String identificacion) {
        Permiso permiso = new Permiso();
        permiso.setArchivo(archivo);
        permiso.setFechaVencimiento(null);
        permiso.setIdentificacionUsuario(identificacion);
        permisoDAO.insertar(permiso);
    }

    public void recibirNotificacionCompartir(final String documentoUsuarioPropietario, final String documentoUsuarioDestino, final Archivo arhivo, final String texto) {

        Usuario usuarioDestino = usuarioEJB.obtenerUsuario(documentoUsuarioDestino);

        CarpetaPersonal carpetaPersonalDestino = usuarioDestino.getCarpetaPersonal();

        Archivo archivoDestino = registrarArchivoRemoto(carpetaPersonalDestino, arhivo, documentoUsuarioPropietario);

        registrarNotificacionCompartidoRemoto(carpetaPersonalDestino, documentoUsuarioPropietario, archivoDestino, texto);

    }

    public FormatoArchivo getFormato(String extencion) {
        FormatoArchivo formato = formatoDAO.buscarPorExtencion(extencion);
        if (formato == null) {
            formato = new FormatoArchivo();
            formato.setExtencion(extencion);
            formato.setNombre(extencion.toUpperCase());
            formatoDAO.insertar(formato);
        }
        return formato;
    }

    public TipoArchivo getTipoArchivo(final String tipo) {
        List<Object> parametros = new ArrayList<>();
        String q = "TipoArchivo.findByNombre";
        parametros.add(tipo);
        return tipoArchivoDAO.buscarNamedQuery(q, parametros);
    }

}
