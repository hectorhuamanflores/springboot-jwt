package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.tomcat.jni.File;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.models.entity.Cliente;
import com.example.demo.models.service.IClienteService;
import com.example.demo.util.paginacion.PageRender;
import com.example.demo.view.xml.ClienteList;


@Controller
@SessionAttributes("cliente")
public class ClienteController {
    
	protected final Log logger = LogFactory.getLog(this.getClass());
	@Autowired
	private IClienteService clienteService;
	@Autowired
	private MessageSource messageSource;
	//private final Logger log = LoggerFactory.getLogger(getClass());
	private final static String UPLOADS_FOLDER ="uploads";
	@Secured("ROLE_USER")
	@RequestMapping(value="/ver/{id}")
	public String ver(@PathVariable(value="id") Long id,Map<String,Object> model,RedirectAttributes flash,Locale locale){
		Cliente cliente = clienteService.findOne(id);
		if(cliente == null){
			flash.addFlashAttribute("error",messageSource.getMessage("text.cliente.flash.db.error", null, locale));
			return "redirect:/listar";
		}
		model.put("cliente", cliente);
		model.put("titulo", messageSource.getMessage("text.cliente.detalle.titulo", null, locale).concat(": ").concat(cliente.getNombre()));
		return "ver";
	}
//  Solo para retornar JSON
//	@GetMapping("/listar-rest")
//	@ResponseBody
//	public List<Cliente> listarRest(){
//		return clienteService.findAll();
//	}
	//Para retornar JSON y XML
	@GetMapping("/listar-rest")
	@ResponseBody
	public ClienteList listarRest(){
		return new ClienteList(clienteService.findAll());
	}
	@RequestMapping( value= {"/listar","/"},method=RequestMethod.GET )
	public String listar(@RequestParam(name="page",defaultValue="0") int page, Model model,
			Authentication authentication,HttpServletRequest request,Locale locale ){
		if(authentication != null){
			logger.info("Hola usuario autenticado, tu username es :".concat(authentication.getName()));
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth != null){
			logger.info("Utilizando forma estática SecurityContextHolder.getContext().getAuthentication(): Usuario autenticado: ".concat(auth.getName()));
		}
		//forma manual
		if(hasRole("ROLE_ADMIN")){
			logger.info("Hola ".concat(auth.getName()).concat(" tienes acceso !"));
		}else{
			logger.info("Hola ".concat(auth.getName()).concat(" No tienes acceso !"));
		}
		//otra forma de validar
		SecurityContextHolderAwareRequestWrapper securityContext = new SecurityContextHolderAwareRequestWrapper(request,"ROLE_");
		if(securityContext.isUserInRole("ADMIN")){
			logger.info("Forma usando SecurityContextHolderAwareRequestWrapper: Hola ".concat(auth.getName()).concat(" tienes acceso !"));
		}else{
			logger.info("Forma usando SecurityContextHolderAwareRequestWrapper: Hola ".concat(auth.getName()).concat(" No tienes acceso !"));
		}
		//form con request
		if(request.isUserInRole("ROLE_ADMIN")){
			logger.info("Forma usando request: Hola ".concat(auth.getName()).concat(" tienes acceso !"));
		}else{
			logger.info("Forma usando request: Hola ".concat(auth.getName()).concat(" No tienes acceso !"));
		}
		
		Pageable pageRequest = PageRequest.of(page, 5);
		Page<Cliente> clientes = clienteService.findAll(pageRequest);
		PageRender<Cliente> pageRender = new PageRender<>("/listar", clientes);
		
//		model.addAttribute("titulo", messageSource.getMessage("text.cliente.listar.titulo", null, locale));
		model.addAttribute("titulo",messageSource.getMessage("text.cliente.listar.titulo", null, locale));
		model.addAttribute("clientes",clientes );
		model.addAttribute("page",pageRender );
		//model.addAttribute("clientes", clienteService.findAll());    sin paginación
		return "listar";
	}
	@Secured("ROLE_ADMIN")
	@RequestMapping(value="/form")
	public String crear(Map<String, Object> model,Locale locale){
		
		Cliente cliente = new Cliente();
		model.put("cliente", cliente);
		model.put("titulo",messageSource.getMessage("text.cliente.form.titulo.crear", null, locale));
		return "form";
	}
	@Secured("ROLE_ADMIN")
	@RequestMapping(value="/form/{id}")
	public String editar(@PathVariable(value="id") Long id,Map<String,Object> model,RedirectAttributes flash,Locale locale){
		Cliente cliente = null;
		if(id>0){
			cliente = clienteService.findOne(id);
			if(cliente == null){
				flash.addFlashAttribute("error", messageSource.getMessage("text.cliente.flash.db.error", null, locale));
				return "redirect:/listar";
			}
		}else{
			flash.addFlashAttribute("error", messageSource.getMessage("text.cliente.flash.id.error", null, locale));
			return "redirect:/listar";
		}
		model.put("cliente", cliente);
		model.put("titulo",messageSource.getMessage("text.cliente.form.titulo.editar", null, locale));
		return "form";
	}
	@Secured("ROLE_ADMIN")
	@RequestMapping( value="/form",method=RequestMethod.POST )
	public String guardar(@Valid Cliente cliente,BindingResult result,Model model,@RequestParam("file") MultipartFile foto, RedirectAttributes flash,SessionStatus status,Locale locale ){
	    
		
		if(result.hasErrors()){
			model.addAttribute("titulo",messageSource.getMessage("text.cliente.form.titulo", null, locale));
			return "form";
		}
		if(!foto.isEmpty()){
//			Path directorioRecursos = Paths.get("src//main//resources//static//uploads");
//			String rootPath = directorioRecursos.toFile().getAbsolutePath();
			if(cliente.getId() != null && cliente.getId()>0 && cliente.getFoto() != null && cliente.getFoto().length()>0){
				Path rootPath = Paths.get(UPLOADS_FOLDER).resolve(cliente.getFoto()).toAbsolutePath();
				java.io.File archivo = rootPath.toFile();
				
				if(archivo.exists() && archivo.canRead()){
					archivo.delete();
				}
			}
			
			String uniqueFilename = UUID.randomUUID().toString()+"_"+foto.getOriginalFilename();
			Path rootPath = Paths.get(UPLOADS_FOLDER).resolve(uniqueFilename);
			Path rootAbsolutPath = rootPath.toAbsolutePath();
			
			//log.info("rootPath: "+rootPath);
			//log.info("rootAbsolutPath: "+rootAbsolutPath);
			try {
//				byte[] bytes = foto.getBytes();
//				Path rutaCompleta = Paths.get(rootPath+"//"+foto.getOriginalFilename());
//				Files.write(rutaCompleta, bytes);
				Files.copy(foto.getInputStream(), rootAbsolutPath);
				flash.addFlashAttribute("info",messageSource.getMessage("text.cliente.flash.foto.subir.success", null, locale)+uniqueFilename+"'");
				cliente.setFoto(uniqueFilename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String mensajeflah = (cliente.getId() != null)?messageSource.getMessage("text.cliente.flash.editar.success", null, locale):messageSource.getMessage("text.cliente.flash.crear.success", null, locale);
		clienteService.save(cliente);
		status.setComplete();
		flash.addFlashAttribute("success", mensajeflah);
	    return "redirect:listar";
	}
	@Secured("ROLE_ADMIN")
	@RequestMapping( value="/eliminar/{id}")
	public String eliminar(@PathVariable(value="id") long id,RedirectAttributes flash,Locale locale){
		if(id>0){
			Cliente cliente = clienteService.findOne(id);
			clienteService.delete(id);
			flash.addFlashAttribute("success", messageSource.getMessage("text.cliente.flash.eliminar.success", null, locale));
			
			Path rootPath = Paths.get(UPLOADS_FOLDER).resolve(cliente.getFoto()).toAbsolutePath();
			java.io.File archivo = rootPath.toFile();
			
			if(archivo.exists() && archivo.canRead()){
				if(archivo.delete()){
					String mensajeFotoEliminar = String.format(messageSource.getMessage("text.cliente.flash.foto.eliminar.success", null, locale), cliente.getFoto());
					flash.addFlashAttribute("info", mensajeFotoEliminar);
				}
			}
		}
		return "redirect:/listar";
	}
	
	private boolean hasRole(String role){
		
		SecurityContext context = SecurityContextHolder.getContext();
		if(context == null){
			return false;
		}
		Authentication auth = context.getAuthentication();
		if(auth == null){
			return false;
		}
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
		for( GrantedAuthority authority:authorities){
			if(role.equals(authority.getAuthority())){
				logger.info("Hola usuario ".concat(auth.getName()).concat(" tu role es: ".concat(authority.getAuthority())));
				return true;
			}
		}
		return false;
	}
}
