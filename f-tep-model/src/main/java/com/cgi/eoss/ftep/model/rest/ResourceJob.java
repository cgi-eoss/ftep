
package com.cgi.eoss.ftep.model.rest;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Links;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Type("jobs")
public class ResourceJob {

    @Id
    private String id;

    // Attributes in HTTP request
    private String jobId;
    private String userId;

    // Attributes in req. and resp.
    private String inputs;
    private String outputs;
    private String guiEndpoint;
    private String serviceName;
    private String step;

    // Attributes in resp.
    private String status;

    @Links
    private com.github.jasminb.jsonapi.Links links;

    @Override
    public String toString() {
        return "Jobs{" + "id='" + id + '\'' + ", jobId='" + jobId + '\'' + ", userId=" + userId
                + ", inputs=" + inputs + ", outputs=" + outputs + ", guiEndpoint=" + guiEndpoint
                + ", serviceName=" + serviceName + ", step=" + step + ", status=" + status + '}';
    }

}
