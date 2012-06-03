package rgms



class Member {
    
    String name
    String username
    String passwordHash
    String email
    String additionalInfo
    String status
    String university
    String phone
    String website
    String city
    String country
    Boolean active
    Boolean enabled
    
    static hasMany = [roles: Role, permissions: String, #if( $History ) historics: Record,#end memberships : Membership, publications: Publication]
    

    
    Date dateCreated
    Date lastUpdated
    def beforeInsert = {
        dateCreated = new Date()
        initializeVelocity()
    }
    def beforeUpdate = {
        lastUpdated = new Date()
    }
    boolean passwordChangeRequiredOnNextLogon
    
    static constraints = {
        name(nullable: false, blank: false)
        username(unique:true,nullable: false, blank: false,size: 5..20)
        email(unique:true,email: true)
        additionalInfo(nullable:true)
        status(nullable: false, inList: ["Graduate Student", "MSc Student", "PhD Student", "Professor", "Researcher"])
        enabled(blank: false)
        university(nullable: false, blank: false)
        phone(nullable:true, blank: true)
        website(nullable:true, url:true, blank: true)
        city(nullable:true, blank: true)
        country(nullable:true, blank: true)
        active(nullable:true, blank: true)
        
    }

    static mapping = {
        cache true
        cache roles : true
        cache permissions : true
        
        #if($History)
        historics fetch: 'join'
        #end
        
        //        historics joinTable: [name: 'USER_HIST',column: 'HIST_ID',key: 'id']
    }

    String toString(){return this.name}
    
  
}
