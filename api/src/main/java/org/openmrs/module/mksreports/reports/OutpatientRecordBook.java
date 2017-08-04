package org.openmrs.module.mksreports.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.mksreports.MKSReportsConstants;
import org.openmrs.module.mksreports.data.converter.AddressAndPhoneConverter;
import org.openmrs.module.mksreports.data.converter.DistanceFromHealthCenterConverter;
import org.openmrs.module.mksreports.data.converter.GenderConverter;
import org.openmrs.module.mksreports.definition.data.CalculatedObsDataDefinition;
import org.openmrs.module.mksreports.definition.data.CalculatedObsDataDefinition.Operator;
import org.openmrs.module.mksreports.definition.data.ContactInfoDataDefinition;
import org.openmrs.module.mksreports.definition.data.ObsOnAgeDataDefinition;
import org.openmrs.module.reporting.common.Age;
import org.openmrs.module.reporting.common.AgeRange;
import org.openmrs.module.reporting.common.MessageUtil;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.common.TimeQualifier;
import org.openmrs.module.reporting.data.converter.AgeRangeConverter;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.patient.library.BuiltInPatientDataLibrary;
import org.openmrs.module.reporting.data.person.definition.AgeDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonAttributeDataDefinition;
import org.openmrs.module.reporting.data.visit.definition.ObsForVisitDataDefinition;
import org.openmrs.module.reporting.data.visit.definition.VisitDataDefinition;
import org.openmrs.module.reporting.data.visit.library.BuiltInVisitDataLibrary;
import org.openmrs.module.reporting.dataset.definition.VisitDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.query.visit.definition.BasicVisitQuery;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.manager.BaseReportManager;
import org.openmrs.module.reporting.report.manager.ReportManagerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OutpatientRecordBook extends BaseReportManager {
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private BuiltInPatientDataLibrary builtInPatientData;
	
	@Autowired
	private BuiltInVisitDataLibrary builtInVisitData;
	
	@Override
	public String getUuid() {
		return "6c74e2ab-0e9b-4469-8901-8221f7d4b498";
	}
	
	@Override
	public String getName() {
		return "HIS Outpatient Record Book";
	}
	
	@Override
	public String getDescription() {
		return "";
	}
	
	private Parameter getStartDateParameter() {
		return new Parameter("startDate", "Start Date", Date.class);
	}
	
	private Parameter getEndDateParameter() {
		return new Parameter("endDate", "End Date", Date.class);
	}
	
	private Parameter getGuardianNameParameter() {
		return new Parameter("guardian", "Guardian Person Attribute Type", PersonAttributeType.class);
	}
	
	private Parameter getDistanceFromHCParameter() {
		return new Parameter("distanceFromHC", "Distance from Health Center Attribute Type", PersonAttributeType.class);
	}
	
	private Parameter getSymptomsParameter() {
		return new Parameter("symptoms", "Symptoms Concept", Concept.class);
	}
	
	private Parameter getDiagnosisParameter() {
		return new Parameter("diagnosis", "Diagnosis Concept", Concept.class);
	}
	
	private Parameter getHeightParameter() {
		return new Parameter("height", "Height Concept", Concept.class);
	}
	
	private Parameter getWeightParameter() {
		return new Parameter("weight", "Weight Concept", Concept.class);
	}
	
	private Parameter getReferredFromParameter() {
		return new Parameter("referredFrom", "Referred From Concept", Concept.class);
	}
	
	private Parameter getReferredToParameter() {
		return new Parameter("referredTo", "Referred To Concept", Concept.class);
	}
	
	private Parameter getPastMedicalHistoryParameter() {
		return new Parameter("pastMedicalHistory", "Past Medical History Concept", Concept.class);
	}
	
	@Override
	public List<Parameter> getParameters() {
		List<Parameter> params = new ArrayList<Parameter>();
		params.add(getStartDateParameter());
		params.add(getEndDateParameter());
		params.add(getGuardianNameParameter());
		params.add(getDistanceFromHCParameter());
		params.add(getSymptomsParameter());
		params.add(getDiagnosisParameter());
		params.add(getHeightParameter());
		params.add(getWeightParameter());
		params.add(getReferredFromParameter());
		params.add(getReferredToParameter());
		params.add(getPastMedicalHistoryParameter());
		return params;
	}
	
	public OutpatientRecordBook() {
	};
	
	@Override
	public ReportDefinition constructReportDefinition() {
		
		ReportDefinition rd = new ReportDefinition();
		rd.setUuid(getUuid());
		rd.setName(getName());
		rd.setDescription(getDescription());
		
		rd.setParameters(getParameters());
		
		VisitDataSetDefinition vdsd = new VisitDataSetDefinition();
		vdsd.addParameters(getParameters());
		rd.addDataSetDefinition("visits", Mapped.mapStraightThrough(vdsd));
		
		BasicVisitQuery query = new BasicVisitQuery();
		
		Parameter endedOnOrAfter = new Parameter("endedOnOrAfter", "Ended On Or After", Date.class);
		Parameter endedBefore = new Parameter("endedOnOrBefore", "Ended On Or Before", Date.class);
		query.setParameters(Arrays.asList(endedOnOrAfter, endedBefore));
		
		{
			Map<String, Object> parameterMappings = new HashMap<String, Object>();
			parameterMappings.put("endedOnOrAfter", "${startDate}");
			parameterMappings.put("endedOnOrBefore", "${endDate}");
			vdsd.addRowFilter(query, ObjectUtil.toString(parameterMappings, "=", ","));
		}
		// Visit ID
		VisitDataDefinition vdd = builtInVisitData.getVisitId();
		vdsd.addColumn("Visit ID", vdd, ObjectUtil.toString(Mapped.straightThroughMappings(vdd), "=", ","));
		
		// Patient ID
		vdsd.addColumn("Patient ID", builtInPatientData.getPatientId(),
		    ObjectUtil.toString(Mapped.straightThroughMappings(builtInPatientData.getPatientId()), "=", ","));
		
		// Patient Identifier
		PatientIdentifierType type = patientService
		        .getPatientIdentifierTypeByUuid(MKSReportsConstants.PATIENT_IDENTIFIER_TYPE_UUID);
		PatientIdentifierDataDefinition pidd = new PatientIdentifierDataDefinition();
		pidd.addType(type);
		
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.identifier.label"), pidd,
		    ObjectUtil.toString(Mapped.straightThroughMappings(pidd), "=", ","));
		
		// Guardian Name
		PersonAttributeDataDefinition paDD1 = new PersonAttributeDataDefinition();
		paDD1.addParameter(new Parameter("personAttributeType", "Person Attribute Type", PersonAttributeType.class));
		{
			Map<String, Object> parameterMappings = new HashMap<String, Object>();
			parameterMappings.put("personAttributeType", "${guardian}");
			vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.guardianName.label"), paDD1,
			    ObjectUtil.toString(parameterMappings, "=", ","));
		}
		
		String isOfCategoryLabel = MessageUtil.translate("mksreports.report.outpatientRecordBook.isOfCategory.label");
		
		// Distance from Health Center zones
		PersonAttributeDataDefinition paDD2 = new PersonAttributeDataDefinition();
		
		PersonAttributeType distanceFromHCAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid(
		    MKSReportsConstants.DISTANCE_FROM_HC_PERSON_ATTRIBUTE_TYPE_UUID);
		paDD2.addParameter(new Parameter("personAttributeType", "Person Attribute Type", PersonAttributeType.class));
		
		Concept distanceFromHCConcept = Context.getConceptService().getConcept(distanceFromHCAttributeType.getForeignKey());
		// Dynamically create the columns based on the Distance From HC concept
		if (distanceFromHCConcept != null) {
			for (ConceptAnswer answer : distanceFromHCConcept.getAnswers()) {
				Concept zone = answer.getAnswerConcept();
				DistanceFromHealthCenterConverter zoneConverter = new DistanceFromHealthCenterConverter(Arrays.asList(zone),
				        isOfCategoryLabel, "");
				Map<String, Object> parameterMappings = new HashMap<String, Object>();
				parameterMappings.put("personAttributeType", "${distanceFromHC}");
				vdsd.addColumn(zone.getShortNameInLocale(Context.getLocale()).getName(), paDD2,
				    ObjectUtil.toString(parameterMappings, "=", ","), zoneConverter);
			}
		}
		
		// Age Categories
		AgeDataDefinition ageDD = new AgeDataDefinition();
		
		AgeRangeConverter ageConverter1 = new AgeRangeConverter();
		ageConverter1.addAgeRange(new AgeRange(0, Age.Unit.MONTHS, 1, Age.Unit.MONTHS, isOfCategoryLabel));
		AgeRangeConverter ageConverter2 = new AgeRangeConverter();
		ageConverter2.addAgeRange(new AgeRange(1, Age.Unit.MONTHS, 12, Age.Unit.MONTHS, isOfCategoryLabel));
		AgeRangeConverter ageConverter3 = new AgeRangeConverter();
		ageConverter3.addAgeRange(new AgeRange(1, Age.Unit.YEARS, 4, Age.Unit.YEARS, isOfCategoryLabel));
		AgeRangeConverter ageConverter4 = new AgeRangeConverter();
		ageConverter4.addAgeRange(new AgeRange(5, Age.Unit.YEARS, 14, Age.Unit.YEARS, isOfCategoryLabel));
		AgeRangeConverter ageConverter5 = new AgeRangeConverter();
		ageConverter5.addAgeRange(new AgeRange(15, Age.Unit.YEARS, 25, Age.Unit.YEARS, isOfCategoryLabel));
		AgeRangeConverter ageConverter6 = new AgeRangeConverter();
		ageConverter6.addAgeRange(new AgeRange(25, Age.Unit.YEARS, 50, Age.Unit.YEARS, isOfCategoryLabel));
		AgeRangeConverter ageConverter7 = new AgeRangeConverter();
		ageConverter7.addAgeRange(new AgeRange(50, Age.Unit.YEARS, 65, Age.Unit.YEARS, isOfCategoryLabel));
		AgeRangeConverter ageConverter8 = new AgeRangeConverter();
		ageConverter8.addAgeRange(new AgeRange(65, Age.Unit.YEARS, 999, Age.Unit.YEARS, isOfCategoryLabel));
		
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory1.label"), ageDD,
		    (String) null, ageConverter1);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory2.label"), ageDD,
		    (String) null, ageConverter2);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory3.label"), ageDD,
		    (String) null, ageConverter3);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory4.label"), ageDD,
		    (String) null, ageConverter4);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory5.label"), ageDD,
		    (String) null, ageConverter5);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory6.label"), ageDD,
		    (String) null, ageConverter6);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory7.label"), ageDD,
		    (String) null, ageConverter7);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.ageCategory8.label"), ageDD,
		    (String) null, ageConverter8);
		
		// Gender categories
		GenderConverter maleConverter = new GenderConverter(Arrays.asList("M"), isOfCategoryLabel, null);
		GenderConverter femaleConverter = new GenderConverter(Arrays.asList("F"), isOfCategoryLabel, null);
		GenderConverter otherConverter = new GenderConverter(Arrays.asList("O"), isOfCategoryLabel, null);
		
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.genderCategoryMale.label"),
		    builtInPatientData.getGender(), (String) null, maleConverter);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.genderCategoryFemale.label"),
		    builtInPatientData.getGender(), (String) null, femaleConverter);
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.genderCategoryOther.label"),
		    builtInPatientData.getGender(), (String) null, otherConverter);
		
		// Address and phone
		ContactInfoDataDefinition ciDD = new ContactInfoDataDefinition();
		AddressAndPhoneConverter addressAndPhoneConverter = new AddressAndPhoneConverter();
		vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.addressAndPhone.label"), ciDD,
		    (String) null, addressAndPhoneConverter);
		
		ObsForVisitDataDefinition obsDD = new ObsForVisitDataDefinition();
		obsDD.setParameters(Arrays.asList(new Parameter("question", "Question", Concept.class)));
		
		// Referred From (Referred From observation)
		{
			Map<String, Object> parameterMappings = new HashMap<String, Object>();
			parameterMappings.put("question", "${referredFrom}");
			vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.referredFrom.label"), obsDD,
			    ObjectUtil.toString(parameterMappings, "=", ","));
		}
		
		// Symptoms (Chief complaint observation)
		{
			Map<String, Object> parameterMappings = new HashMap<String, Object>();
			parameterMappings.put("question", "${symptoms}");
			vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.symptoms.label"), obsDD,
			    ObjectUtil.toString(parameterMappings, "=", ","));
		}
		
		// Diagnosis (Diagnosis observation)
		{
			Map<String, Object> parameterMappings = new HashMap<String, Object>();
			parameterMappings.put("question", "${diagnosis}");
			vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.diagnosis.label"), obsDD,
			    ObjectUtil.toString(parameterMappings, "=", ","));
		}
		
		// Nutritional Weight:Height
		{
			ObsForVisitDataDefinition weightDD = new ObsForVisitDataDefinition();
			// Use the most recent Weight
			weightDD.setWhich(TimeQualifier.LAST);
			weightDD.addParameter(new Parameter("question", "Question", Concept.class));
			Map<String, Object> weightMappings = new HashMap<String, Object>();
			weightMappings.put("question", "${question1}");
			Mapped<ObsForVisitDataDefinition> mappedWeightDD = new Mapped<ObsForVisitDataDefinition>();
			mappedWeightDD.setParameterizable(weightDD);
			mappedWeightDD.setParameterMappings(weightMappings);
			
			ObsForVisitDataDefinition heightDD = new ObsForVisitDataDefinition();
			// Use the most recent Height
			heightDD.setWhich(TimeQualifier.LAST);
			heightDD.addParameter(new Parameter("question", "Question", Concept.class));
			Map<String, Object> heightMappings = new HashMap<String, Object>();
			heightMappings.put("question", "${question2}");
			Mapped<ObsForVisitDataDefinition> mappedHeightDD = new Mapped<ObsForVisitDataDefinition>();
			mappedHeightDD.setParameterizable(weightDD);
			mappedHeightDD.setParameterMappings(heightMappings);
			
			{
				Map<String, Object> parameterMappings = new HashMap<String, Object>();
				parameterMappings.put("question1", "${weight}");
				parameterMappings.put("question2", "${height}");
				
				CalculatedObsDataDefinition calculatedDD = new CalculatedObsDataDefinition();
				calculatedDD.setOperator(Operator.DIVISION);
				calculatedDD.setObsDefinition1(mappedWeightDD);
				calculatedDD.setObsDefinition2(mappedHeightDD);
				
				// Adding 2 parameters on the calculatedDD in order to ensure that question1 and question2
				// are passed to its properties 'weightDD' and 'heightDD'
				// These parameters do not have to match an existing property of the CalculatedObsDataDefinition
				// They are just used to pass the 'weight' and 'height' report params
				calculatedDD.addParameter(new Parameter("question1", "Question 1", Concept.class));
				calculatedDD.addParameter(new Parameter("question2", "Question 2", Concept.class));
				
				vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.weightOnHeight.label"),
				    calculatedDD, ObjectUtil.toString(parameterMappings, "=", ","));
			}
		}
		
		// Nutritional Weight/Age
		{
			ObsForVisitDataDefinition obsVisitDD = new ObsForVisitDataDefinition();
			// Use the most recent Weight
			obsVisitDD.setWhich(TimeQualifier.LAST);
			obsVisitDD.addParameter(new Parameter("question", "question", Concept.class));
			
			AgeDataDefinition agePersonDD = new AgeDataDefinition();
			agePersonDD.addParameter(new Parameter("effectiveDate", "Effective Date", Date.class));
			{
				Map<String, Object> parameterMappings = new HashMap<String, Object>();
				parameterMappings.put("question", "${weight}");
				parameterMappings.put("effectiveDate", "${endDate}");
				
				ObsOnAgeDataDefinition obsOnAge = new ObsOnAgeDataDefinition();
				obsOnAge.setObsDefinition(Mapped.mapStraightThrough(obsVisitDD));
				obsOnAge.setAgeDefinition(Mapped.mapStraightThrough(agePersonDD));
				obsOnAge.addParameter(new Parameter("question", "Question", Concept.class));
				obsOnAge.addParameter(new Parameter("effectiveDate", "Effective Date", Date.class));
				
				vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.weightOnAge.label"), obsOnAge,
				    ObjectUtil.toString(parameterMappings, "=", ","));
			}
		}
		
		// Referred To
		{
			Map<String, Object> parameterMappings = new HashMap<String, Object>();
			parameterMappings.put("question", "${referredTo}");
			vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.referredTo.label"), obsDD,
			    ObjectUtil.toString(parameterMappings, "=", ","));
		}
		
		// Other notes (Past medical history observation)
		{
			Map<String, Object> parameterMappings = new HashMap<String, Object>();
			parameterMappings.put("question", "${pastMedicalHistory}");
			vdsd.addColumn(MessageUtil.translate("mksreports.report.outpatientRecordBook.otherNotes.label"), obsDD,
			    ObjectUtil.toString(parameterMappings, "=", ","));
		}
		
		return rd;
	}
	
	@Override
	public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
		ReportDesign design = ReportManagerUtil.createCsvReportDesign("9873e45d-f8a0-4682-be78-243b8c9b848c",
		    reportDefinition);
		List<ReportDesign> list = new ArrayList<ReportDesign>();
		list.add(design);
		return list;
	}
	
	@Override
	public String getVersion() {
		return "0.1.0-SNAPSHOT";
	}
	
}
