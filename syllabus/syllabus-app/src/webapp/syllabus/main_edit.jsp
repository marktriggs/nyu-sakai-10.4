<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://sakaiproject.org/jsf/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/syllabus" prefix="syllabus" %>
<% response.setContentType("text/html; charset=UTF-8"); %>
<script type="text/javascript" src="js/jquery-1.9.1.js"></script>
<script type="text/javascript" src="js/jquery-ui-1.10.1.custom.min.js"></script>
<link type="text/css" href="/library/js/jquery/jquery-ui/css/smoothness/jquery-ui.css" rel="stylesheet" media="screen" />
<script type="text/javascript" src="js/jquery-ui-timepicker-addon.js"></script>
<script type="text/javascript" src="js/syllabus.js"></script>
<link type="text/css" href="syllabus/css/syllabus.css" rel="stylesheet" media="screen" />
 <f:view>
<script>
  var startDateValues = new Array();
  var dateFormat = '<h:outputText value="#{msgs.jqueryDatePickerDateFormat}"/>';
  var timeFormat = '<h:outputText value="#{msgs.jqueryDatePickerTimeFormat}"/>';
  var dataChanged = false;
  var msgs = {
			saved: "<h:outputText value="#{msgs.saved}"/>",
			error: "<h:outputText value="#{msgs.error}"/>"
		};

  $(function() {
    setupSortableEdit(msgs);
    $('.dateInput').datetimepicker({
    	hour: 8,
		timeFormat: timeFormat,
		currentText: "<h:outputText value="#{msgs.now}"/>",
		closeText: "<h:outputText value="#{msgs.done}"/>",
		amNames: ['<h:outputText value="#{msgs.am}"/>', '<h:outputText value="#{msgs.am2}"/>'],
		pmNames: ['<h:outputText value="#{msgs.pm}"/>', '<h:outputText value="#{msgs.pm2}"/>'],
		timeText: "<h:outputText value="#{msgs.time}"/>",
		hourText: "<h:outputText value="#{msgs.hour}"/>",
		minuteText: "<h:outputText value="#{msgs.minute}"/>",
		monthNames: ["<h:outputText value="#{msgs.jan}"/>",
					  "<h:outputText value="#{msgs.feb}"/>",
					  "<h:outputText value="#{msgs.mar}"/>",
					  "<h:outputText value="#{msgs.apr}"/>",
					  "<h:outputText value="#{msgs.may}"/>",
					  "<h:outputText value="#{msgs.jun}"/>",
					  "<h:outputText value="#{msgs.jul}"/>",
					  "<h:outputText value="#{msgs.aug}"/>",
					  "<h:outputText value="#{msgs.sep}"/>",
					  "<h:outputText value="#{msgs.oct}"/>",
					  "<h:outputText value="#{msgs.nov}"/>",
					  "<h:outputText value="#{msgs.dec}"/>"],
		dayNames: ["<h:outputText value="#{msgs.sunday}"/>",
							"<h:outputText value="#{msgs.monday}"/>",
							"<h:outputText value="#{msgs.tuesday}"/>",
							"<h:outputText value="#{msgs.wednesday}"/>",
							"<h:outputText value="#{msgs.thursday}"/>",
							"<h:outputText value="#{msgs.friday}"/>",
							"<h:outputText value="#{msgs.saturday}"/>"],
		dayNamesMin: ["<h:outputText value="#{msgs.sun}"/>",
							"<h:outputText value="#{msgs.mon}"/>",
							"<h:outputText value="#{msgs.tue}"/>",
							"<h:outputText value="#{msgs.wed}"/>",
							"<h:outputText value="#{msgs.thu}"/>",
							"<h:outputText value="#{msgs.fri}"/>",
							"<h:outputText value="#{msgs.sat}"/>"],
		beforeShow: function (textbox, instance) {
			            instance.dpDiv.css({
			                    marginLeft: textbox.offsetWidth + 'px'
			          });
		
	}
	});
  
  
  	
  	//Setup the current values of the start dates (to compare and adjust the end dates when changed)
  	$(".dateInputStart").each(function(){
		startDateValues[$(this).attr('id')] = $(this).val();
  	});
  	//setup onchange event for startDate changes
  	$(".dateInputStart").change(function(){
  		var startDate = new Date($(this).val());
  		var prevStartDate = new Date(startDateValues[$(this).attr('id')]);
  		var endDate = new Date($(this).closest("tr").find(".dateInputEnd").val());
  		if(isNaN(startDate.getTime()) == false && isNaN(prevStartDate.getTime()) == false && isNaN(endDate.getTime()) == false){
  			//we only want to update if all three of these dates have been set
  			var timeDiff = startDate.getTime() - prevStartDate.getTime();
  			var newEndDate = new Date(endDate.getTime() + timeDiff);
  			var newEndTime = {hour: newEndDate.getHours(), minute: newEndDate.getMinutes()};
  			$(this).closest("tr").find(".dateInputEnd").val($.datepicker.formatDate(dateFormat, newEndDate) + " " + $.datepicker.formatTime(timeFormat, newEndTime));
  		}
  		startDateValues[$(this).attr('id')] = $(this).val();
  	});
  	
  	//setup data change listener
  	$('input').change(function() {
    	dataChanged = true;
	});
  	
  	//disable calendar options that are in draft:
  	disableCalendarOptions();
  	//add listeners to the calendar dates for the calendar checkbox:
  	$(".dateInputStart").change(function(){
  		checkCalendarDates(this);
  	});
  	$(".dateInputEnd").change(function(){
  		checkCalendarDates(this);
  	});
  	
  });
  
	function toggleCalendarCheckbox(postCheckbox){
		$(postCheckbox).parent().parent().find(".calendarBox").each(function(){
			if(postCheckbox.checked){
				$(this).removeAttr("disabled");
			}else{
				$(this).attr("disabled", "disabled");
				this.checked = false;
			}
		});
	}
	
	function checkStartEndDates(calendarCheckbox){
		if(calendarCheckbox.checked){
			//check that this rows has either start or end dates set
			var startTime = $(calendarCheckbox).parent().parent().find(".dateInputStart").val();
			var endTime = $(calendarCheckbox).parent().parent().find(".dateInputEnd").val();
			if((startTime == null || "" == $.trim(startTime))
					&& (endTime == null || "" == $.trim(endTime))){
				showMessage("<h:outputText value="#{msgs.calendarDatesNeeded}"/>", false);
				calendarCheckbox.checked = false;
			}
		}
	}
	
	var deleteClick;
            
	function assignWarningClick(link) {
  		if (link.onclick == confirmPost) {
    		return;
  		}
                
  		deleteClick = link.onclick;
  		link.onclick = confirmPost;
	}

	function confirmPost(){
		if(dataChanged){
			var agree=confirm('<h:outputText value="#{msgs.main_edit_confirmDataChanged}"/>');
			if (agree)
				return deleteClick();
			else
				return false ;
		}else{
			return deleteClick();
		}
	}
	
	function toggleAllCalendarOptions(toggleCheckbox){
		$('.calendarBox').each(function(){
			if(toggleCheckbox.checked){
				//make sure that the post option is checked otherwise don't check it as well as the start or end date isn't null
				if($(this).parent().parent().find(".postBox:checked").length == 1){
					var startTime = $(this).parent().parent().find(".dateInputStart").val();
					var endTime = $(this).parent().parent().find(".dateInputEnd").val();
					if((startTime != null && "" != $.trim(startTime)) || (endTime != null && "" != $.trim(endTime))){
						//at least one date is set
						this.checked = true;
					}else{
						showMessage("<h:outputText value="#{msgs.calendarDatesNeededToggle}"/>", false);
					}
				}
			}else{
				this.checked = false;
			}
		});
	}

	function toggleAllPostOptions(toggleCheckbox){
		$('.postBox').each(function(){
			if(toggleCheckbox.checked){
				this.checked = true;
			}else{
				this.checked = false;
				//make sure calendar option is unchecked
				$(this).parent().parent().find(".calendarBox").removeAttr("checked");
			}
			toggleCalendarCheckbox(this);
		});
	}
	
	function disableCalendarOptions(){
		$('.calendarBox').each(function(){
			if($(this).parent().parent().find(".postBox:checked").length == 0){
				$(this).attr("disabled", "disabled");
			}
		});
	}
	
	//used for when a date is changed, it will check that the calendar checkbox is not checked if
	//both dates are empty
	function checkCalendarDates(dateInput){
		var startTime = $(dateInput).parent().parent().find(".dateInputStart").val();
		var endTime = $(dateInput).parent().parent().find(".dateInputEnd").val();
		if((startTime == null || "" == $.trim(startTime)) && (endTime == null || "" == $.trim(endTime))){
			//both start and end dates are null, so make sure the calendar checkbox is unchecked:
			$(dateInput).parent().parent().find(".calendarBox").removeAttr("checked");
		}
	}
 </script>
 <style>
 	td.move{
 		text-align: center;
 	}
 </style>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.tool.syllabus.bundle.Messages"/>
</jsp:useBean>
<div>
	<span id="successInfo" class="success popupMessage" style="display:none; float: left;"></span>
	<span id="warningInfo" class="alertMessage popupMessage" style="display:none; float: left;"></span>
</div>
	<sakai:view_container title="#{msgs.title_list}">
	<sakai:view_content>
		<h:form>
		  <sakai:tool_bar>
		  <%-- (gsilver) cannot pass a needed title attribute to these next items 
		  	<h:commandLink action="#{SyllabusTool.processListNew}" 
		  		styleClass="actionLink" 
		  		onmousedown="assignWarningClick(this);"
			    rendered="#{SyllabusTool.editAble == 'true'}">
			    	<f:verbatim>
			    		<img src="/library/image/silk/add.png"/>&nbsp;&nbsp;
			    	</f:verbatim>
			    	<h:outputText value="#{msgs.bar_new}"/>
			</h:commandLink>
			--%>
		   	<h:commandLink
					action="#{SyllabusTool.processListNewBulkMainEdit}"
					onmousedown="assignWarningClick(this);"
		   			rendered="#{SyllabusTool.editAble == 'true'}">
						<h:outputText value="#{msgs.bar_new}"/>
		   	</h:commandLink>
		   	
			<h:outputText value=" #{msgs.revise} "/>
		   	
		   	<h:commandLink
					action="#{SyllabusTool.processRedirectMainEdit}"
					onmousedown="assignWarningClick(this);"
		   			rendered="#{SyllabusTool.editAble == 'true'}">
						<h:outputText value="#{msgs.bar_redirect}"/>
		   	</h:commandLink>
		   	<h:commandLink
					action="#{SyllabusTool.processStudentView}"
					onmousedown="assignWarningClick(this);"
		   			rendered="#{SyllabusTool.editAble == 'true'}">
			    		<h:outputText value="#{msgs.bar_student_view}"/>
			</h:commandLink>
		   			
   	      </sakai:tool_bar>
   	      <h:messages globalOnly="true" styleClass="alertMessage" rendered="#{!empty facesContext.maximumSeverity}" />
	      <syllabus:syllabus_if test="#{SyllabusTool.syllabusItem.redirectURL}">
		     <sakai:tool_bar_message value="#{msgs.mainEditNotice}" />
		     <syllabus:syllabus_table value="#{SyllabusTool.entries}" var="eachEntry" summary="#{msgs.mainEditListSummary}" styleClass="listHier lines nolines editTable">
<%--						<h:column rendered="#{!empty SyllabusTool.entries}">--%>
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg}">
							<f:facet name="header">
								<h:outputText value="#{msgs.mainEditHeaderItem}" />
							</f:facet>
							<h:inputText value="#{eachEntry.entry.title}"/>
						</h:column>
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg}">
							<f:facet name="header">
								<h:outputText value="#{msgs.title_edit}" />
							</f:facet>
							<h:commandLink action="#{eachEntry.processListRead}" title="#{msgs.goToItem} #{eachEntry.entry.title}" onmousedown="assignWarningClick(this);"><f:verbatim><img src="/library/image/silk/pencil.png"/></f:verbatim></h:commandLink>
						</h:column>
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg}">
							<f:facet name="header">
								<h:outputText value="#{msgs.reorder}" />
							</f:facet>
							<h:graphicImage url="/images/cursor_drag_arrow.png" title="#{msgs.dragToReorder}"  styleClass="actionIcon" style="float:center"/>
							<f:verbatim><span style="display:none;" class="syllabusItem"></f:verbatim><h:outputText value="#{eachEntry.entry.syllabusId}"/><f:verbatim></span></f:verbatim>
						</h:column>
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg}">
							<f:facet name="header">
								<h:outputText value="#{msgs.mainEditHeaderStartTime}"/>
							</f:facet>
							<h:inputText styleClass="dateInput dateInputStart" value="#{eachEntry.entry.startDate}" id="dataStartDate">
								<f:convertDateTime pattern="#{msgs.mainEditHeaderTimeFormat}"/>
							</h:inputText>
							<f:verbatim><img src="/library/image/silk/calendar_view_month.png" onclick="$(this).prev().focus();"/></f:verbatim>
						</h:column>	
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg}">
							<f:facet name="header">
								<h:outputText value="#{msgs.mainEditHeaderEndTime}"/>
							</f:facet>
							<h:inputText styleClass="dateInput dateInputEnd" value="#{eachEntry.entry.endDate}" id="dataEndDate">
								<f:convertDateTime pattern="#{msgs.mainEditHeaderTimeFormat}"/>
							</h:inputText>
							<f:verbatim><img src="/library/image/silk/calendar_view_month.png" onclick="$(this).prev().focus();"/></f:verbatim>
						</h:column>
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg && SyllabusTool.calendarExistsForSite}">
							<f:facet name="header">
								<h:panelGroup>
									<h:outputText value="#{msgs.mainEditHeaderInCalendar}"/>
									<f:verbatim>
										<br/>
										<input type="checkbox" onchange="toggleAllCalendarOptions(this);"/>
									</f:verbatim>
								</h:panelGroup>
							</f:facet>
							<h:selectBooleanCheckbox styleClass="calendarBox" value="#{eachEntry.entry.linkCalendar}" title="#{msgs.selectThisCheckBoxCal}" onchange="checkStartEndDates(this)"/>
						</h:column>
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg}">
							<f:facet name="header">
								<h:panelGroup>
									<h:outputText value="#{msgs.mainEditHeaderStatus}"/>
									<f:verbatim>
										<br/>
										<input type="checkbox" onchange="toggleAllPostOptions(this);"/>
									</f:verbatim>
								</h:panelGroup>
							</f:facet>
							<h:selectBooleanCheckbox styleClass="postBox" value="#{eachEntry.posted}" title="#{msgs.selectThisCheckBoxPost}" onchange="toggleCalendarCheckbox(this);"/>
						</h:column>
						<h:column rendered="#{! SyllabusTool.displayNoEntryMsg}">
							<f:facet name="header">
								<h:panelGroup>
  									<h:outputText value="#{msgs.mainEditHeaderRemove}"/>
  									<f:verbatim>
										<br/>
										<input type="checkbox" onchange="$('.deleteBox').attr('checked', this.checked);"/>
									</f:verbatim>
								</h:panelGroup>
							</f:facet>
							<h:selectBooleanCheckbox styleClass="deleteBox" value="#{eachEntry.selected}" title="#{msgs.selectThisCheckBox}"/>
						</h:column>
			 </syllabus:syllabus_table>
			 <f:verbatim><p class="act"></f:verbatim>	
				<h:commandButton 
				     value="#{msgs.save}"
				     styleClass="active" 
					 action="#{SyllabusTool.processListDelete}"
					 title="#{msgs.save}"
				     rendered="#{! SyllabusTool.displayNoEntryMsg}"
					 accesskey="s" 	/>
				<h:commandButton 
				     value="#{msgs.bar_cancel}" 
					 action="#{SyllabusTool.processMainEditCancel}"
					 title="#{msgs.bar_cancel}"
				     rendered="#{! SyllabusTool.displayNoEntryMsg}"
					 accesskey="r" 	/>
			<f:verbatim></p></f:verbatim>		  
		  </syllabus:syllabus_if>
	      <syllabus:syllabus_ifnot test="#{SyllabusTool.syllabusItem.redirectURL}">
		    <sakai:tool_bar_message value="#{msgs.redirect_sylla}" />
		    <syllabus:syllabus_iframe redirectUrl="#{SyllabusTool.syllabusItem.redirectURL}" width="100%" height="500" />
		  </syllabus:syllabus_ifnot>
		
		<f:verbatim><span style="display:none">
		  	<input type="hidden" id="moveItem" name="moveItem"/>
			<input type="hidden" id="moveSteps" name="moveSteps"/>
		  </f:verbatim>
			<h:commandLink action="#{SyllabusTool.processMove}" styleClass="move">
				<h:outputText value="hidden" styleClass="skip"/>
			</h:commandLink>
		  <f:verbatim></span></f:verbatim>

		</h:form>
	</sakai:view_content>
	</sakai:view_container>
</f:view>
