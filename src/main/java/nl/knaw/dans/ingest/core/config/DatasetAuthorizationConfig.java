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
package nl.knaw.dans.ingest.core.config;

import javax.validation.constraints.NotEmpty;

public class DatasetAuthorizationConfig {
    @NotEmpty
    private String datasetCreator;
    @NotEmpty
    private String datasetUpdater;

    public String getDatasetUpdater() {
        return datasetUpdater;
    }

    public void setDatasetUpdater(String datasetUpdater) {
        this.datasetUpdater = datasetUpdater;
    }

    public String getDatasetCreator() {
        return datasetCreator;
    }

    public void setDatasetCreator(String datasetCreator) {
        this.datasetCreator = datasetCreator;
    }
}
