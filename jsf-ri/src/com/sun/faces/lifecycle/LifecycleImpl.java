/*
 * $Id: LifecycleImpl.java,v 1.10 2002/06/25 21:42:53 eburns Exp $
 */

/*
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// LifecycleImpl.java

package com.sun.faces.lifecycle;

import org.mozilla.util.Assert;
import org.mozilla.util.ParameterCheck;

import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.Phase;
import javax.faces.lifecycle.ApplicationHandler;
import javax.faces.lifecycle.ViewHandler;
import javax.faces.context.FacesContext;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import java.util.Iterator;
import java.util.ArrayList;

/**
 *
 *  <B>LifecycleImpl</B> is the stock implementation of the standard
 *  Lifecycle in the JSF RI. <P>
 *
 *
 * @version $Id: LifecycleImpl.java,v 1.10 2002/06/25 21:42:53 eburns Exp $
 * 
 * @see	javax.faces.lifecycle.Lifecycle
 *
 */

public class LifecycleImpl extends Lifecycle
{
//
// Protected Constants
//

//
// Class Variables
//

//
// Instance Variables
//

// Attribute Instance Variables

// Relationship Instance Variables

/**

* INVARIANT: The contents of phaseWrappers must not change after the
* ctor returns.

*/

protected ArrayList phaseWrappers;


protected Object lock = null;

protected ApplicationHandler applicationHandler = null;
protected ViewHandler viewHandler = null;

//
// Constructors and Initializers    
//

public LifecycleImpl()
{
    super();
    phaseWrappers  = new ArrayList();
    initPhases();
    lock = new Object();
}

//
// Class methods
//

//
// General Methods
//

protected void initPhases()
{
    phaseWrappers.add(new PhaseWrapper(new CreateRequestTreePhase(this, 
					Lifecycle.CREATE_REQUEST_TREE_PHASE)));
    phaseWrappers.add(new PhaseWrapper(new GenericPhaseImpl(this, Lifecycle.RECONSTITUTE_REQUEST_TREE_PHASE,
                 new LifecycleCallback() {
		     public int takeActionOnComponent(FacesContext context,
						      UIComponent component) throws FacesException {
			 // PENDING(edburns): not sure what to do in
			 // this phase
			 // PENDING(edburns): log this
			 System.out.println("RECONSTITUTE_REQUEST_TREE_PHASE:"+
					    " " + component.getComponentId());
			 return Phase.GOTO_NEXT;
		     }
		 }
							    )));
    phaseWrappers.add(new PhaseWrapper(new ApplyRequestValuesPhase(this, 
                                       Lifecycle.APPLY_REQUEST_VALUES_PHASE)));
    phaseWrappers.add(new PhaseWrapper(new HandleRequestEventsPhase(this, 
                                      Lifecycle.HANDLE_REQUEST_EVENTS_PHASE)));
    phaseWrappers.add(new PhaseWrapper(new ProcessValidationsPhase(this, 
  				        Lifecycle.PROCESS_VALIDATIONS_PHASE)));
    phaseWrappers.add(new PhaseWrapper(new UpdateModelValuesPhase(this, 
				        Lifecycle.UPDATE_MODEL_VALUES_PHASE)));
    phaseWrappers.add(new PhaseWrapper(new InvokeApplicationPhase(this, 
					 Lifecycle.INVOKE_APPLICATION_PHASE)));
    phaseWrappers.add(new PhaseWrapper(new JspRenderResponsePhase(this, 
					    Lifecycle.RENDER_RESPONSE_PHASE)));
}

/**

* PRECONDITION: all arguments are valid.

*/

protected void registerBefore(int phaseId, Phase phase)
{
    Assert.assert_it(null != phaseWrappers);

    Phase genericPhase = null;
    PhaseWrapper wrapper = null;
   
    // PENDING (visvan) Ed: please review
    Iterator it = phaseWrappers.iterator();
    while ( it.hasNext() ) {
        wrapper = (PhaseWrapper) it.next();
        Assert.assert_it(wrapper != null);
        genericPhase = wrapper.instance;
        Assert.assert_it(genericPhase != null);
        if ( (((GenericPhaseImpl)genericPhase).getId()) == phaseId) {
            break;
        }
    }    
    
    Assert.assert_it(wrapper != null);
    wrapper.registerBefore(phase);
}

/**

* PRECONDITION: all arguments are valid.

*/

protected void registerAfter(int phaseId, Phase phase)
{
    Assert.assert_it(null != phaseWrappers);

    Phase genericPhase = null;
    PhaseWrapper wrapper = null;
  
    // PENDING (visvan) Ed: please review
    Iterator it = phaseWrappers.iterator();
    while ( it.hasNext() ) {
        wrapper = (PhaseWrapper) it.next();
        Assert.assert_it(wrapper != null);
        genericPhase = wrapper.instance;
        Assert.assert_it(genericPhase != null);
        if ( (((GenericPhaseImpl)genericPhase).getId()) == phaseId) {
            break;
        }
    }    
    
    Assert.assert_it(wrapper != null);
    wrapper.registerAfter(phase);
}

protected int executeRender(FacesContext context) throws FacesException
{
    Assert.assert_it(null != phaseWrappers);
    int rc = 0;
    Phase renderPhase = null;
    PhaseWrapper wrapper = null;
    
    // PENDING (visvan) Ed: please review
    Iterator it = phaseWrappers.iterator();
    while ( it.hasNext() ) {
        wrapper = (PhaseWrapper) it.next();
        Assert.assert_it(wrapper != null);
        renderPhase = wrapper.instance;
        Assert.assert_it(renderPhase != null);
        if ( (((GenericPhaseImpl)renderPhase).getId()) == 
                Lifecycle.RENDER_RESPONSE_PHASE) {
            break;
        }
    }
    
    Assert.assert_it(null != renderPhase);
    rc = renderPhase.execute(context);
    
    return rc;
}

//
// Methods from Lifecycle
//

public ApplicationHandler getApplicationHandler()
{
    return applicationHandler;
}

public void setApplicationHandler(ApplicationHandler handler)
{
    applicationHandler = handler;
}

public ViewHandler getViewHandler()
{
    if (null == viewHandler) {
	viewHandler = new ViewHandlerImpl();
    }
    return viewHandler;
}

public void setViewHandler(ViewHandler handler)
{
    ParameterCheck.nonNull(handler);

    viewHandler = handler;
}

public void execute(FacesContext context) throws FacesException
{
    PhaseWrapper wrapper = null;
    Phase curPhase = null;
    Iterator phaseIter = phaseWrappers.iterator();
    int curPhaseId = 0, rc = 0;
    Assert.assert_it(null != phaseIter);

    while (phaseIter.hasNext()) {
	wrapper = (PhaseWrapper)phaseIter.next();
	curPhase = wrapper.instance;

	// Execute the before phases for the current phase, if any
	if (Phase.GOTO_EXIT == (rc = wrapper.executeBefore(context))) {
	    return;
	}
	else if (rc == Phase.GOTO_RENDER) {
	    executeRender(context);
	    return;
	}

	// Execute the current phase
	if (Phase.GOTO_EXIT == (rc = curPhase.execute(context))) {
	    return;
	}
	else if (rc == Phase.GOTO_RENDER) {
	    executeRender(context);
	    return;
	}

	// Execute the after phases for the current phase, if any
	if (Phase.GOTO_EXIT == (rc = wrapper.executeAfter(context))) {
	    return;
	}
	else if (rc == Phase.GOTO_RENDER) {
	    executeRender(context);
	    return;
	}
	curPhaseId++;
    }
}

public int executePhase(FacesContext context, Phase phase) throws FacesException
{
    if (null == context || null == phase) { 
	throw new NullPointerException("Null FacesContext"); 
    } 
    
    return phase.execute(context); 
}


//
// Helper classes
//

static class PhaseWrapper extends Object
{

protected Phase instance = null;
protected ArrayList beforeList = null;
protected ArrayList afterList = null;

PhaseWrapper(Phase newInstance)
{
    instance = newInstance;
}

/**

* PRECONDITION: phaseIter is non null <P>

* POSTCONDITION: All the phases in phaseIter have had their execute
* method called, unless one of them either threw an exception or
* returned a result other than Phase.GOTO_NEXT.

*/

private int executePhaseIterator(Iterator phaseIter, 
				 FacesContext context) throws FacesException
{
    Phase curPhase = null;
    int rc = Phase.GOTO_NEXT;
    while (phaseIter.hasNext()) {
	curPhase = (Phase) phaseIter.next();
	rc = curPhase.execute(context);
	if (Phase.GOTO_NEXT != rc) {
	    return rc;
	}
    }
    return rc;
}

int executeBefore(FacesContext context) throws FacesException
{
    if (null == beforeList) {
	return Phase.GOTO_NEXT;
    }
    Iterator phaseIter = beforeList.iterator();
    int rc = 0;
    Assert.assert_it(null != phaseIter);
    rc = executePhaseIterator(phaseIter, context);
    return rc;
}

int executeAfter(FacesContext context) throws FacesException
{
    if (null == afterList) {
	return Phase.GOTO_NEXT;
    }
    Iterator phaseIter = afterList.iterator();
    int rc = 0;
    Assert.assert_it(null != phaseIter);
    rc = executePhaseIterator(phaseIter, context);
    return rc;
}

/**

* PRECONDITION: phase is valid.

*/

void registerBefore(Phase phase)
{
    if (null == beforeList) {
	beforeList = new ArrayList();
    }
    beforeList.add(phase);
}

/**

* PRECONDITION: phase is valid.

*/

void registerAfter(Phase phase)
{
    if (null == afterList) {
	afterList = new ArrayList();
    }
    afterList.add(phase);
}

} // end of class PhaseWrapper


// The testcase for this class is TestLifecycleImpl.java 


} // end of class LifecycleImpl
