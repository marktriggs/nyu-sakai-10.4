<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://sakaiproject.org/jsf/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/syllabus" prefix="syllabus" %>
<% response.setContentType("text/html; charset=UTF-8"); %>
<script type="text/javascript" src="js/jquery-1.9.1.js"></script>
<script type="text/javascript" src="js/jquery-ui-1.10.1.custom.min.js"></script>
<link type="text/css" href="/library/js/jquery/jquery-ui/css/smoothness/jquery-ui.css" rel="stylesheet" media="screen" />
<script type="text/javascript" src="js/jquery-ui-timepicker-addon.js"></script>

<f:view>
	<script>
		$(function() {
			$(".dateInput").datepicker({
				beforeShow: function (textbox, instance) {
					            instance.dpDiv.css({
					                    marginLeft: textbox.offsetWidth + 'px'
					            });
					        }
			});
			$('.timeInput').timepicker({
		    	hour: 8,
		    	defaultValue: "08:00 <h:outputText value="#{msgs.am}"/>",
		    	timeOnlyTitle: "<h:outputText value="#{msgs.choose_time}"/>",
				timeFormat: "hh:mm tt",
				currentText: "<h:outputText value="#{msgs.now}"/>",
				closeText: "<h:outputText value="#{msgs.done}"/>",
				amNames: ['<h:outputText value="#{msgs.am}"/>', '<h:outputText value="#{msgs.am2}"/>'],
				pmNames: ['<h:outputText value="#{msgs.pm}"/>', '<h:outputText value="#{msgs.pm2}"/>'],
				timeText: "<h:outputText value="#{msgs.time}"/>",
				hourText: "<h:outputText value="#{msgs.hour}"/>",
				minuteText: "<h:outputText value="#{msgs.minute}"/>",
				beforeShow: function (textbox, instance) {
					            instance.dpDiv.css({
					                    marginLeft: textbox.offsetWidth + 'px'
								});
							}
			});
			
			//radio options:
			$('.radioByOne input:radio').change(
				function(){
					$('.radioByItems input:radio').each(function(i){
						this.checked = false;
					});
					$('.radioByDate input:radio').each(function(i){
						this.checked = false;
					});
					$('.radioOption').each(function(i){
						$(this).removeClass("radioOptionSelected");
					});
					$('.radioByOne').each(function(i){
						$(this).addClass("radioOptionSelected");
					});
					$('.bulkAddByItemsPanel').slideUp();
					$('.bulkAddByDatePanel').slideUp();
				}
			);
			$('.radioByDate input:radio').change(
				function(){
					$('.radioByItems input:radio').each(function(i){
						this.checked = false;
					});
					$('.radioByOne input:radio').each(function(i){
						this.checked = false;
					});
					$('.radioOption').each(function(i){
						$(this).removeClass("radioOptionSelected");
					});
					$('.radioByDate').each(function(i){
						$(this).addClass("radioOptionSelected");
					});
					$('.bulkAddByItemsPanel').slideUp();
					$('.bulkAddByDatePanel').slideDown();
					resizeFrame('grow');
				}
			);
			$('.radioByItems input:radio').change(
				function(){
					$('.radioByDate input:radio').each(function(i){
						this.checked = false;
					});
					$('.radioByOne input:radio').each(function(i){
						this.checked = false;
					});
					$('.radioOption').each(function(i){
						$(this).removeClass("radioOptionSelected");
					});
					$('.radioByItems').each(function(i){
						$(this).addClass("radioOptionSelected");
					});
					$('.bulkAddByItemsPanel').slideDown();
					$('.bulkAddByDatePanel').slideUp();
					resizeFrame('grow');
				}
			);
			if($('.radioByDate input:radio').is(':checked')){
				//date option is selected... we need to setup the UI
				//this can happen if a user gets a warning message when
				//setting up the dates options
				$('.radioByDate input:radio').each(function(){
					$('.radioByItems input:radio').each(function(i){
						this.checked = false;
					});
					$('.radioOption').each(function(i){
						$(this).removeClass("radioOptionSelected");
					});
					$('.radioByDate').each(function(i){
						$(this).addClass("radioOptionSelected");
					});
					$('.bulkAddByItemsPanel').hide();
					$('.bulkAddByDatePanel').show();
					resizeFrame('grow');
				});
			}
		});
		//this function needs jquery 1.1.2 or later - it resizes the parent iframe without bringing the scroll to the top
		function resizeFrame(updown){
		    var clientH;
		    if (top.location != self.location) {
		        var frame = parent.document.getElementById(window.name);
		    }
		    if (frame) {
		        if (updown == 'shrink') {
		            clientH = document.body.clientHeight - 200;
		        }
		        else {
		            clientH = document.body.clientHeight + 200;
		        }
		        $(frame).height(clientH);
		    }
		    else {
		        throw ("resizeFrame did not get the frame (using name=" + window.name + ")");
		    }
		}
	</script>
	<style>
		.radioOption{
			background: none repeat scroll 0 0 #EEEEEE;
			border-radius: 5px 5px 5px 5px;
			padding: .5em;
			width: 35em;
		}
		
		.radioOptionSelected{
			background: none repeat scroll 0 0 #CCCCCC;
		}
		
		#ui-datepicker-div { display:none }
		
		.widerLabel{
			width: 20em !important;	
		}
	</style>
	<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
		<jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.tool.syllabus.bundle.Messages"/>
	</jsp:useBean>
	<sakai:view_container title="#{msgs.title_edit_bulk}">
		<sakai:view_content>
			<h:outputText value="#{SyllabusTool.alertMessage}" styleClass="alertMessage" rendered="#{SyllabusTool.alertMessage != null}" />
				
			<sakai:tool_bar_message value="#{msgs.add_sylla_bulk}" /> 
			<sakai:doc_section>
				<h:outputText value="#{msgs.newSyllabusBulkForm}"/>
			</sakai:doc_section>
			<h:form>
				<h:panelGrid columns="1" styleClass="jsfFormTable">
					<h:panelGroup styleClass="shorttext">
						<h:outputLabel for="title">
							<h:outputText value="*" styleClass="reqStar"/>
							<h:outputText value="#{msgs.syllabus_title}"/>
						</h:outputLabel>
						<h:inputText value="#{SyllabusTool.bulkEntry.title}" id="title"/>
					</h:panelGroup>
					
					<h:panelGroup>
						<f:verbatim><div class="radioByOne radioOption radioOptionSelected"></f:verbatim>
							<h:selectOneRadio id="radioByOne" value="#{SyllabusTool.bulkEntry.addByOne}">
								<f:selectItem id="byOne" itemLabel="#{msgs.bulkAddByOne}" itemValue="1" />
							</h:selectOneRadio>
							<h:outputText value="#{msgs.bulkAddByOneInfo}" style="font-style: italic;"/>
						<f:verbatim></div></f:verbatim>
					</h:panelGroup>
					<h:panelGroup>
						<f:verbatim>
							<div style="border-radius: 0 !important; padding: .5em; background: #ccc; width: 35em;">
								<img src="/library/image/sakai/collapse.gif" border="0" width="13" height="13" align="top">
						</f:verbatim>
						<h:outputText value="#{msgs.bulkAddAdvancedOptions} ("/>
						<f:verbatim>
							<a href="http://www.nyu.edu/servicelink/041212917134698" target="_blank">
						</f:verbatim>
							<h:outputText value="#{msgs.bulkAddAdvancedOptionsLink}"/>	
						<f:verbatim>	
							</a>
						</f:verbatim>
						<h:outputText value=")"/>
						<f:verbatim>
							</div>
						</f:verbatim>
					</h:panelGroup>
					
					<h:panelGroup>
						<f:verbatim><div class="radioByItems radioOption"></f:verbatim>
							<h:selectOneRadio id="radioByItems" value="#{SyllabusTool.bulkEntry.addByItems}">
								<f:selectItem id="byItems" itemLabel="#{msgs.bulkAddByItems}" itemValue="1" />
							</h:selectOneRadio>
						<f:verbatim></div></f:verbatim>
					</h:panelGroup>
					
					<!-- Add X Bulk Entries -->
					<h:panelGrid columns="1" styleClass="jsfFormTable indnt1 bulkAddByItemsPanel" style="display: none">
						<h:panelGroup styleClass="shorttext">
							<h:outputLabel for="numOfItems" styleClass="widerLabel">
								<h:outputText value="#{msgs.numberOfItems}"/><h:outputText styleClass="instruction" value=" #{msgs.blankCreatesOne}"/>								
							</h:outputLabel>
							<h:inputText value="#{SyllabusTool.bulkEntry.bulkItems}" id="numOfItems" size="3" maxlength="3"/>
						</h:panelGroup>
					</h:panelGrid>
					<h:panelGroup>
						<f:verbatim><div class="radioByDate radioOption"></f:verbatim>
							<h:selectOneRadio id="radioByDate" value="#{SyllabusTool.bulkEntry.addByDate}">
								<f:selectItem id="byDate" itemLabel="#{msgs.bulkAddByDate}" itemValue="1" />
							</h:selectOneRadio>
						<f:verbatim></div></f:verbatim>
					</h:panelGroup>
					<!-- Add Bulk Entries by date -->
					<h:panelGrid columns="1" styleClass="jsfFormTable indnt1 bulkAddByDatePanel" style="display: none">
						<h:panelGroup styleClass="shorttext">
							<h:outputLabel for="dataStartDate">
								<h:outputText value="*" styleClass="reqStar"/>
								<h:outputText value="#{msgs.startdatetitle}"/>
							</h:outputLabel>
							<h:inputText styleClass="dateInput datInputStart" value="#{SyllabusTool.bulkEntry.startDateString}" id="dataStartDate"/>
							<f:verbatim><img src="/library/image/silk/calendar_view_month.png" onclick="$('.datInputStart').focus();"/></f:verbatim>
						</h:panelGroup>
						<h:panelGroup styleClass="shorttext">
							<h:outputLabel for="dataEndDate">
								<h:outputText value="*" styleClass="reqStar"/>
								<h:outputText value="#{msgs.enddatetitle}"/>
							</h:outputLabel>
							<h:inputText styleClass="dateInput datInputEnd" value="#{SyllabusTool.bulkEntry.endDateString}" id="dataEndDate"/>
							<f:verbatim><img src="/library/image/silk/calendar_view_month.png" onclick="$('.datInputEnd').focus();"/></f:verbatim>
						</h:panelGroup>
						<h:panelGroup styleClass="shorttext">
							<h:outputLabel for="dataStartTime">
								<h:outputText value="*" styleClass="reqStar"/>
								<h:outputText value="#{msgs.starttimetitle}"/>
							</h:outputLabel>
							<h:inputText styleClass="timeInput timeInputStart" value="#{SyllabusTool.bulkEntry.startTimeString}" id="dataStartTime"/>
							<f:verbatim><img src="/library/image/silk/calendar_view_month.png" onclick="$('.timeInputStart').focus();"/></f:verbatim>
						</h:panelGroup>
						<h:panelGroup styleClass="shorttext">
							<h:outputLabel for="dataEndTime">
								<h:outputText value="#{msgs.endtimetitle}"/>
							</h:outputLabel>
							<h:inputText styleClass="timeInput timeInputEnd" value="#{SyllabusTool.bulkEntry.endTimeString}" id="dataEndTime"/>
							<f:verbatim><img src="/library/image/silk/calendar_view_month.png" onclick="$('.timeInputEnd').focus();"/></f:verbatim>
						</h:panelGroup>
						<h:panelGroup styleClass="shorttext" rendered="#{SyllabusTool.calendarExistsForSite}">
							<h:selectBooleanCheckbox id="linkCalendar" value="#{SyllabusTool.bulkEntry.linkCalendar}" />
							<h:outputLabel for="linkCalendar">
								<h:outputText value="#{msgs.linkcalendartitle}"/>
							</h:outputLabel>
						</h:panelGroup>
						<h:panelGroup styleClass="shorttext">
							<h:selectBooleanCheckbox id="monday" value="#{SyllabusTool.bulkEntry.monday}" />
							<h:outputText value="#{msgs.monday}"/>
							<h:outputText value=" | "/>
							<h:selectBooleanCheckbox id="tuesday" value="#{SyllabusTool.bulkEntry.tuesday}" />
							<h:outputText value="#{msgs.tuesday}"/>
							<h:outputText value=" | "/>
							<h:selectBooleanCheckbox id="wednesday" value="#{SyllabusTool.bulkEntry.wednesday}" />
							<h:outputText value="#{msgs.wednesday}"/>
							<h:outputText value=" | "/>
							<h:selectBooleanCheckbox id="thursday" value="#{SyllabusTool.bulkEntry.thursday}" />
							<h:outputText value="#{msgs.thursday}"/>
							<h:outputText value=" | "/>
							<h:selectBooleanCheckbox id="friday" value="#{SyllabusTool.bulkEntry.friday}" />
							<h:outputText value="#{msgs.friday}"/>
							<h:outputText value=" | "/>
							<h:selectBooleanCheckbox id="saturday" value="#{SyllabusTool.bulkEntry.saturday}" />
							<h:outputText value="#{msgs.saturday}"/>
							<h:outputText value=" | "/>
							<h:selectBooleanCheckbox id="sunday" value="#{SyllabusTool.bulkEntry.sunday}" />
							<h:outputText value="#{msgs.sunday}"/>
							<h:outputLabel for="monday">
								<h:outputText value="*" styleClass="reqStar"/>
								<h:outputText value="#{msgs.classMeetingDays}"/>
							</h:outputLabel>
						</h:panelGroup>
					</h:panelGrid>
				</h:panelGrid>
				<sakai:button_bar>
					<sakai:button_bar_item
						action="#{SyllabusTool.processEditBulkPost}"
						styleClass="active"
						value="#{msgs.continue}" 
						accesskey="s"
						title="#{msgs.continue}" />
					<sakai:button_bar_item
						action="#{SyllabusTool.processEditBulkCancel}"
						value="#{msgs.cancel}" 
						accesskey="x"
						title="#{msgs.button_cancel}" />
				</sakai:button_bar>
			</h:form>
		</sakai:view_content>
	</sakai:view_container>

</f:view>
