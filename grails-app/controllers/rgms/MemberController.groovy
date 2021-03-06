package rgms

import org.apache.shiro.crypto.hash.Sha256Hash
import org.springframework.dao.DataIntegrityViolationException
import java.security.SecureRandom
import rgms.*

class MemberController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
    
    def index = {
        redirect(action: "list", params: params)
    }

    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [memberInstanceList: Member.list(params), memberInstanceTotal: Member.count()]
    }

    def create = {
		def member = new Member(params)
		def defaultValues = DefaultValueManager.getInstance()
		member.setEmail("@"+defaultValues.getPropertyValue(DefaultValueManager.Domain))
		member.setUniversity(defaultValues.getPropertyValue(DefaultValueManager.Univeristy))
		member.setCountry(defaultValues.getPropertyValue(DefaultValueManager.Country))
		member.setCity(defaultValues.getPropertyValue(DefaultValueManager.City))
		
        [memberInstance: member]
    }

    def save = {
       // #if( $Auth )
        if (!grailsApplication.config.grails.mail.username) {
            throw new RuntimeException(message(code: 'mail.plugin.not.configured', 'default' : 'Mail plugin not configured'))
        }
        //#end
		
        def memberInstance = new Member(params)
        def username = memberInstance?.username
        def password = ""
        
        if (!memberInstance.passwordHash) {
            
            password = new BigInteger(130, new SecureRandom()).toString(32)
            memberInstance.passwordHash = new Sha256Hash(password).toHex()
        }
        memberInstance.passwordChangeRequiredOnNextLogon = true
        
        //feature record
        //#if($History)
            
        saveHistory();
           //end feature record
       //#end
        
        if (!memberInstance.save(flush: true)) { 
            render(view: "create", model: [memberInstance: memberInstance])
            return
        }
        
        sendMail {
            to memberInstance.email
            from grailsApplication.config.grails.mail.username
            subject "[GRMS] Your account was successfully created!"
            body "Hello ${ memberInstance.name},\n\nYour account was successfully created!\n\nHere is your username: ${ username} and password: ${ password}\n\n${ createLink(absolute:true,uri:'/')}\n\nBest Regards,\nAdministrator of the Research Group Management System".toString()
        }
        
        flash.message = message(code: 'default.created.message', args: [message(code: 'member.label', default: 'Member'), memberInstance.id])
        redirect(action: "show", id: memberInstance.id)
    }


	def pdfPeriodico () { 
		def member = Member.get(params.id)
		
		def listaPeriodico = new ArrayList()
		Periodico.findAll().each {
		    if(it.members.contains(member)){
				listaPeriodico.add(it)
			}
		}

		PdfController pdf = new PdfController()
		return pdf.index(listaPeriodico)
	}
	
	def pdfConferencia () {
		def member = Member.get(params.id)
		
		def listaConferencia = new ArrayList()
		Conferencia.findAll().each {
			if(it.members.contains(member)){
				listaConferencia.add(it)
			}
		}

		PdfController pdf = new PdfController()
		return pdf.index(listaConferencia)
	}
	
	def pdfFerramenta () {
		def member = Member.get(params.id)
		
		def listaFerramenta = new ArrayList()
		Ferramenta.findAll().each {
			if(it.members.contains(member)){
				listaFerramenta.add(it)
			}
		}

		PdfController pdf = new PdfController()
		return pdf.index(listaFerramenta)
	}
	
	def pdfDissertacao () {
		def member = Member.get(params.id)
		
		def listaDissertacao = new ArrayList()
		Ferramenta.findAll().each {
			if(it.members.contains(member)){
				listaDissertacao.add(it)
			}
		}

		PdfController pdf = new PdfController()
		return pdf.index(listaDissertacao)
	}
	
	def pdfTese () {
		def member = Member.get(params.id)
		
		def listaTese = new ArrayList()
		Ferramenta.findAll().each {
			if(it.members.contains(member)){
				listaTese.add(it)
			}
		}

		PdfController pdf = new PdfController()
		return pdf.index(listaTese)
	}
	
    def show() {
        def memberInstance = Member.get(params.id)
        if (!memberInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'member.label', default: 'Member'), params.id])
            redirect(action: "list")
            return
        }

        [memberInstance: memberInstance]
    }

    def edit = {
        def memberInstance = Member.get(params.id)
        if (!memberInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'member.label', default: 'Member'), params.id])
            redirect(action: "list")
            return
        }

        [memberInstance: memberInstance]
    }

    def update = {
        def memberInstance = Member.get(params.id)
        
        if (!memberInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'member.label', default: 'Member'), params.id])
            redirect(action: "list")
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (memberInstance.version > version) {
                memberInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                    [message(code: 'member.label', default: 'Member')] as Object[],
                          "Another user has updated this Member while you were editing")
                render(view: "edit", model: [memberInstance: memberInstance])
                return
            }
        }

        //feature record
        //#if($History)
        def status0 = memberInstance.status //pega o status anterior do usuario
        //#end
        
        
        memberInstance.properties = params //atualiza todos os parametros

        if (!memberInstance.save(flush: true)) {
            render(view: "edit", model: [memberInstance: memberInstance])
            return
        }
        
        //feature record
        // #if($History)
        
        //#if($History) //feature record

        String newStatus = memberInstance.status //pega o novo status
        
        //salva o historico se o status mudar
        if (newStatus != status0){
            try{
                def hist = Record.findWhere(end: null, status_H:status0)
                hist.end = new Date()
            
                def h = Record.merge(hist)
                h.save()
                memberInstance.addToHistorics(h)
            } catch(Exception ex){
                render "You do not have permission to access this account."
            }
            saveHistory(memberInstance, newStatus) //refactoring - extract method
        }
        //end feature record
        // #end
        
        flash.message = message(code: 'default.updated.message', args: [message(code: 'member.label', default: 'Member'), memberInstance.id])
        redirect(action: "show", id: memberInstance.id)
    }

    def delete = {
        def memberInstance = Member.get(params.id)
        if (!memberInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'member.label', default: 'Member'), params.id])
            redirect(action: "list")
            return
        }

        try {
            memberInstance.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'member.label', default: 'Member'), params.id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'member.label', default: 'Member'), params.id])
            redirect(action: "show", id: params.id)
        }
    }
    
    private void saveHistory(def memberInstance, String status){
        
            def hist = new Record(start: new Date(), status_H: status)
            hist.save()
            
            memberInstance.addToHistorics(hist)
            memberInstance.save()
    }
}
