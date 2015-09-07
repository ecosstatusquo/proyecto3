/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.edu.uniandes.ecos.statusquo.centralizador.ws.servicios;

import co.edu.uniandes.ecos.statusquo.centralizador.ejb.UsuarioBean;
import co.edu.uniandes.ecos.statusquo.centralizador.persistence.entity.Usuario;
import co.edu.uniandes.ecos.statusquo.centralizador.ws.service.respuesta.ContextoRespuestaTipo;
import co.edu.uniandes.ecos.statusquo.centralizador.ws.servicios.common.FalloTipo;
import co.edu.uniandes.ecos.statusquo.centralizador.ws.servicios.common.Servicio;
import co.edu.uniandes.ecos.statusquo.centralizador.ws.servicios.usuariows.RespuestaGetDocumentoUsuarioWS;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 *
 * @author Dev
 */
@WebService(serviceName = "UsuarioSW")
public class UsuarioSW extends Servicio {

    @EJB
    private UsuarioBean usuarioEJB;
    
    @WebMethod(operationName = "setUsuario")
    public ContextoRespuestaTipo setUsuario(@WebParam(name = "usuario") Usuario user) throws FalloTipo {
        ContextoRespuestaTipo resp = new ContextoRespuestaTipo();
        try {
            usuarioEJB.createUsuario(user);
            //resp.getRespuestaEstandar().setIdTx(null); TODO agregar id del modulo de seguridad encargado del registro de movimientos
            resp.setCodEstadoTx("1");
            resp.setFechaTx(getFecha());
        } catch (Exception ex) {
            controlFallo("500", "Error al consultar el usuario", ex.getMessage());
        }
        if (!errores.isEmpty()) {
            resp.setCodEstadoTx("0");
            throw new FalloTipo(resp, errores);
        }
        return resp;
    }

    @WebMethod(operationName = "getUsuario")
    public RespuestaGetDocumentoUsuarioWS getDocumento(@WebParam(name = "identificacion") String identificacion) throws FalloTipo {
        RespuestaGetDocumentoUsuarioWS resp = new RespuestaGetDocumentoUsuarioWS();
        try {
            resp.setUsuario(usuarioEJB.consultarIdentificacion(identificacion));
            //resp.getRespuestaEstandar().setIdTx(null); TODO agregar id del modulo de seguridad encargado del registro de movimientos
            resp.getRespuestaEstandar().setCodEstadoTx("1");
            resp.getRespuestaEstandar().setFechaTx(getFecha());
        } catch (Exception ex) {
            controlFallo("500", "Error al consultar el usuario", ex.getMessage());
        }
        if (!errores.isEmpty()) {
            resp.getRespuestaEstandar().setCodEstadoTx("0");
            throw new FalloTipo(resp.getRespuestaEstandar(), errores);
        }
        return resp;
    }
}