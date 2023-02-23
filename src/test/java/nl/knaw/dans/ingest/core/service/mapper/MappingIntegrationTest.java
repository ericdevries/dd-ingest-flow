/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.ingest.core.service.mapper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.CONTRIBUTOR_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.NOTES_TEXT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SUBJECT;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.dcmi;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.ddmProfileWithAudiences;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getCompoundMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getControlledMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getPrimitiveSingleValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.mapDdmToDataset;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.readDocumentFromString;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.rootAttributes;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.toCompactJsonString;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.toPrettyJsonString;
import static org.assertj.core.api.Assertions.assertThat;

class MappingIntegrationTest {

    @Test
    void DD_1216_description_type_other_maps_only_to_author_name() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
            + ddmProfileWithAudiences("D24000")
            + dcmi("<ddm:description descriptionType=\"Other\">Author from description other</ddm:description>\n")
            + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false, false);
        var field = getCompoundMultiValueField("citation", "contributor", result);
        var expected = "Author from description other";
        assertThat(field).extracting(CONTRIBUTOR_NAME).extracting("value")
            .containsOnly(expected);
        // not as description and author
        assertThat(toPrettyJsonString(result)).containsOnlyOnce(expected);
    }

    @Test
    void DD_1216_description_type_technical_info_maps_once_to_description() throws Exception {
        String dcmiContent = "<dct:description>plain description</dct:description>\n"
            + "<ddm:description descriptionType=\"TechnicalInfo\">technical description</ddm:description>\n"
            + "<ddm:description descriptionType=\"NotKnown\">not known description type</ddm:description>\n";
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
                + ddmProfileWithAudiences("D24000")
                + dcmi(dcmiContent)
                + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false, false);
        var str = toPrettyJsonString(result);
        assertThat(str).containsOnlyOnce("not known description type");
        assertThat(str).containsOnlyOnce("technical description");
        assertThat(str).containsOnlyOnce("Lorem ipsum");
        var field = getCompoundMultiValueField("citation", DESCRIPTION, result);
        assertThat(field).extracting(DESCRIPTION_VALUE).extracting("value")
            .containsOnly("<p>plain description</p>", "<p>Lorem ipsum.</p>", "<p>technical description</p>", "<p>not known description type</p>");
    }

    @Test
    void DD_1216_description_type_series_information_maps_only_to_series() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
                + ddmProfileWithAudiences("D24000")
                + dcmi("<ddm:description descriptionType=\"SeriesInformation\">series 123</ddm:description>\n")
                + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false, false);

        // TODO improve assertions after DD-1237 (note that the single compound field is an anonymous class)
        //  {"typeClass" : "compound", "typeName" : "series", "multiple" : false, "value" :
        //  {"seriesName" : {"typeClass" : "primitive", "typeName" : "seriesInformation", "multiple" : false, "value" : "<p>series 123</p>"}}
        //  }
        var str = toCompactJsonString(result);

        // not as description and series
        assertThat(str).containsOnlyOnce("<p>series 123</p>");

        // no square bracket
        assertThat(str).containsOnlyOnce("\"value\":{\"seriesInformation\"");
    }

    @Test
    void DD_1216_provenance_maps_to_notes() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
                + ddmProfileWithAudiences("D24000")
                + dcmi("<dct:provenance>copied xml to csv</dct:provenance>\n")
                + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false, false);
        var str = toPrettyJsonString(result);

        assertThat(str).containsOnlyOnce("copied xml to csv");
        assertThat(str).doesNotContain("<p>copied xml to csv</p>");

        assertThat(getPrimitiveSingleValueField("citation", NOTES_TEXT, result))
            .isEqualTo("copied xml to csv");
    }

    @Test
    void DD_1265_subject_omits_other() throws Exception {
        var doc = readDocumentFromString(""
            +"<ddm:DDM " + rootAttributes + ">\n"
            + ddmProfileWithAudiences("D19200", "D11200", "D88200", "D40200", "D17200")
            + dcmi("")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getControlledMultiValueField("citation", SUBJECT, result))
            .isEqualTo(List.of("Astronomy and Astrophysics","Law", "Mathematical Sciences"));
    }

    @Test
    void DD_1265_subject_is_other() throws Exception {
        var doc = readDocumentFromString(""
            +"<ddm:DDM " + rootAttributes + ">\n"
            + ddmProfileWithAudiences("D19200", "D88200")
            + dcmi("")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getControlledMultiValueField("citation", SUBJECT, result))
            .isEqualTo(List.of("Other"));
    }

    @Test
    void DD_1216_DctAccesRights_maps_to_description() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
                + ddmProfileWithAudiences("D10000")
                + dcmi("<dct:accessRights>Some story</dct:accessRights>\n")
                + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false, false);
        var str = toPrettyJsonString(result);

        assertThat(str).containsOnlyOnce("<p>Some story</p>");

        var field = getCompoundMultiValueField("citation", DESCRIPTION, result);
        assertThat(field).extracting(DESCRIPTION_VALUE).extracting("value")
            .containsOnly("<p>Some story</p>", "<p>Lorem ipsum.</p>");
        assertThat(result.getDatasetVersion().getTermsOfAccess()).isEqualTo("N/a");
    }

    @Test
    void DD_1216_DctAccesRights_maps_to_termsofaccess() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
                + ddmProfileWithAudiences("D10000")
                + dcmi("<dct:accessRights>Some story</dct:accessRights>\n")
                + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, true, true);
        assertThat(result.getDatasetVersion().getTermsOfAccess()).isEqualTo("Some story");
    }
}