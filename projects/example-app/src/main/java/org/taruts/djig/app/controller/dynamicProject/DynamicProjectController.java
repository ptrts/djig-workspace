package org.taruts.djig.app.controller.dynamicProject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.taruts.djig.app.dynamicWiring.DynamicProject;
import org.taruts.djig.app.dynamicWiring.DynamicProjectManager;

@RestController
@RequestMapping("dynamic-project")
public class DynamicProjectController {

    @Autowired
    private DynamicProjectManager dynamicProjectManager;

    @PutMapping("{projectName}")
    public void put(@PathVariable("projectName") String projectName, @RequestBody DynamicProjectDto dto) {
        DynamicProject dynamicProject = DynamicProjectDtoMapper.map(projectName, dto);
        dynamicProjectManager.addProject(dynamicProject);
    }
}
