package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.demo.auth.handler.LoginSuccesHandler;
import com.example.demo.models.service.JpaUserDetailsService;


//@Secured("ROLE_USER") o @Secured({"ROLE_USER","ROLE_ADMIN"}) tambien podriamos usar
//  @EnableGlobalMethodSecurity(prePostEnabled=true)
//  @PreAuthorize("hasRole('ROLE_USER')") o @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN','ROLE_OTRO')")
@EnableGlobalMethodSecurity(securedEnabled=true,prePostEnabled=true)
@Configuration
public class SpringSecurityConfig  extends WebSecurityConfigurerAdapter{
    @Autowired
	private LoginSuccesHandler successHandler;

    @Autowired
    private JpaUserDetailsService userDetailsService;
    
	@Autowired
    private BCryptPasswordEncoder passwordEncoder;
	
	//autentificamos los http (accesos)
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
		 .antMatchers("/","/css/**","/js/**","/images/**","/listar**","/locale").permitAll()
		 /*.antMatchers("/ver/**").hasAnyRole("USER")*/
		 .antMatchers("/uploads/**").hasAnyRole("USER")
		 /*.antMatchers("/form/**").hasAnyRole("ADMIN")
		 .antMatchers("/eliminar/**").hasAnyRole("ADMIN")
		 .antMatchers("/factura/**").hasAnyRole("ADMIN")*/
		 .anyRequest().authenticated()
		 .and()
		 .formLogin()
		     .successHandler(successHandler)
		     .loginPage("/login")
		 .permitAll()
		 .and()
		 .logout().permitAll()
		 .and()
		 .exceptionHandling().accessDeniedPage("/error_403");
	}
    //Generamos los roles
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder build) throws Exception{
		build.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
	}
}
