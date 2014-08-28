package org.openmrs.module.shr.odd.api.db.hibernate;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Patient;
import org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;

/**
 * On-demand document DAO class
 */
public class HibernateOnDemandDocumentDAO implements OnDemandDocumentDAO {
	
	// Hibernate session factory
	private SessionFactory m_sessionFactory;
	
	/**
	 * Save an on demand document registration entry
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#saveOnDemandDocumentRegistration(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public OnDemandDocumentRegistration saveOnDemandDocumentRegistration(OnDemandDocumentRegistration document) {
		this.m_sessionFactory.getCurrentSession().saveOrUpdate(document);
		return document;
	}
	
	/**
	 * Get an on demand document registration by ID
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#getOnDemandDocumentRegistrationById(java.lang.Integer)
	 */
	@Override
	public OnDemandDocumentRegistration getOnDemandDocumentRegistrationById(Integer id) {
		return (OnDemandDocumentRegistration)this.m_sessionFactory.getCurrentSession().get(OnDemandDocumentRegistration.class, id);
	}
	
	/**
	 * Get on demand document registration by UUID
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#getOnDemandDocumentRegistrationByUuid(java.lang.String)
	 */
	@Override
	public OnDemandDocumentRegistration getOnDemandDocumentRegistrationByUuid(String uuid) {
		return this.getClassByUuid(OnDemandDocumentRegistration.class, uuid);
	}
	
	/**
	 * Get ODD by accession number 
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#getOnDemandDocumentRegistrationByAccessionNumber(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByAccessionNumber(String accessionNumber, boolean includeVoided) {
		Criteria crit = this.m_sessionFactory.getCurrentSession().createCriteria(OnDemandDocumentRegistration.class)
				.add(Restrictions.eq("accessionNumber", accessionNumber));
		if(!includeVoided)
				crit.add(Restrictions.eq("voided", includeVoided));
		return (List<OnDemandDocumentRegistration>)crit.list();
	}

	/**
	 * Get on-demand document registration
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#getOnDemandDocumentRegistrationsByPatient(org.openmrs.Patient)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByPatient(Patient patient, boolean includeVoided) {
		Criteria crit = this.m_sessionFactory.getCurrentSession().createCriteria(OnDemandDocumentRegistration.class)
				.add(Restrictions.eq("patient", patient));
		
		if(!includeVoided)
			crit.add(Restrictions.eq("voided", includeVoided));
		return (List<OnDemandDocumentRegistration>)crit.list();
	}
	
	/**
	 * Get on-demand document type by id
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#getOnDemandDocumentTypeById(java.lang.Integer)
	 */
	@Override
	public OnDemandDocumentType getOnDemandDocumentTypeById(Integer id) {
		return (OnDemandDocumentType) this.m_sessionFactory.getCurrentSession().get(OnDemandDocumentType.class, id);
	}
	
	/**
	 * Get an on-demand document type by UUID
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#getOnDemandDocumentTypeByUuid(java.lang.String)
	 */
	@Override
	public OnDemandDocumentType getOnDemandDocumentTypeByUuid(String uuid) {
		return this.getClassByUuid(OnDemandDocumentType.class, uuid);
	}
	
	/**
	 * Save an on-demand document type
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#saveOnDemandDocumentType(org.openmrs.module.shr.odd.model.OnDemandDocumentType)
	 */
	@Override
	public OnDemandDocumentType saveOnDemandDocumentType(OnDemandDocumentType documentType) {
		this.m_sessionFactory.getCurrentSession().saveOrUpdate(documentType);
		return documentType;
	}
	
	/**
	 * Get all links for a document encounter
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#getOnDemandDocumentEncounterLinks(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public List<OnDemandDocumentEncounterLink> getOnDemandDocumentEncounterLinks(OnDemandDocumentRegistration document, boolean includeVoided) {
		Criteria crit = this.m_sessionFactory.getCurrentSession().createCriteria(OnDemandDocumentEncounterLink.class)
				.add(Restrictions.eq("registration", document));
		if(!includeVoided)
			crit.add(Restrictions.eq("voided", includeVoided));
		return (List<OnDemandDocumentEncounterLink>)crit.list();
	}
	
	/**
	 * Save an on demand document to encounter link
	 * @see org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO#saveOnDemandDocumentEncounterLink(org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink)
	 */
	@Override
	public OnDemandDocumentEncounterLink saveOnDemandDocumentEncounterLink(OnDemandDocumentEncounterLink link) {
		this.m_sessionFactory.getCurrentSession().saveOrUpdate(link);
		return link;
	}

	
    /**
     * @param sessionFactory the sessionFactory to set
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
    	this.m_sessionFactory = sessionFactory;
    }

    /**
     * Get class by UUID
     */
    @SuppressWarnings("unchecked")
    private <T> T getClassByUuid(Class<T> clazz, String uuid)
    {
    	return (T)this.m_sessionFactory.getCurrentSession().createCriteria(clazz).add(Restrictions.eq("uuid", uuid)).uniqueResult();
    }
}
