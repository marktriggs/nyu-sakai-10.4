package org.sakaiproject.evaluation.tool.viewparams;

import uk.org.ponder.rsf.viewstate.SimpleViewParameters;

public class HierarchyNodeParameters extends SimpleViewParameters {
    public String nodeId;
    public String[] expanded = null;
    
    public HierarchyNodeParameters() {
    }
    
    public HierarchyNodeParameters(String viewID, String nodeId, String[] expanded) {
        this.viewID = viewID;
        this.nodeId = nodeId;
        this.expanded = expanded;
    }
}
