
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.archive.ArchiveFormats;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.primefaces.component.tree.Tree;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author rajendra.dave
 */
@ManagedBean(name = "autoProjectGenerator")
@ViewScoped
public class AutoProjectGenerator {

    public Map<String, String> frameworkList = new HashMap<String, String>();
    public Git git;
    public String framework;
    private String frontEndFramework = StringUtils.EMPTY;
    private String backEndFramework = StringUtils.EMPTY;
    private String service = StringUtils.EMPTY;
    private String frontEndAppName = StringUtils.EMPTY;
    private String backEndAppName = StringUtils.EMPTY;
    private String serviceAppName = StringUtils.EMPTY;
    
    private Map<String,String> frontEndFrameworks = new LinkedHashMap<>();
    private Map<String,String> backEndFrameworks = new LinkedHashMap<>();
    private Map<String,String> services = new LinkedHashMap<>();
    @PostConstruct
    public void init() {
        fetchBranchList();
        fetchFrameworks();
    }

    public void fetchBranchList() {
        String repoUrl = "https://github.com/omsai90/Boiler-Plate.git";
        String cloneDirectoryPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + File.separator +"testCheckout";
        try {
            File cloneDir = new File(cloneDirectoryPath);
            if (cloneDir.exists()) {
                git = Git.open(new File(cloneDirectoryPath));
                git.fetch().call();
                git.pull().call();

            } else {
                FileUtils.forceMkdir(cloneDir);
                git = Git.cloneRepository().setURI(repoUrl).setBranch("refs/remotes/origin/master")
                    .setDirectory(Paths.get(cloneDirectoryPath).toFile()).call();
            }

            List<Ref> call = git.branchList().setListMode(ListMode.ALL).call();
            for(Ref ref : call) {
                String[] refBranchName = ref.getName().split("/");
                String branchName = refBranchName[refBranchName.length - 1];
                if(!branchName.equalsIgnoreCase("master")){
                    frameworkList.put(branchName, ref.getName());
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void fetchFrameworks() {
        frontEndFrameworks = new LinkedHashMap<>();
        frontEndFrameworks.put("Django", "django");
        frontEndFrameworks.put("Angular", "angular");
        frontEndFrameworks.put("Java Server Faces", "JSF");
        
        backEndFrameworks = new LinkedHashMap<>();
        backEndFrameworks.put("Django", "django");
        
        services = new LinkedHashMap<>();
        services.put("Strapi", "strapi");
        
    }
    
    public void download(String framework){
        createArchieve(git, framework);
    }

    public void createArchieve(Git git, String branchName) {
        try {
            File file = File.createTempFile("test", ".zip");
            ArchiveFormats.registerAll();
            try (OutputStream out = new FileOutputStream(file)) {
                git.archive().setTree(git.getRepository().resolve(branchName)).setFormat("zip")
                        .setOutputStream(out).call();
            }
            System.out.println("Completed Cloning");
            downLoadFile(file, branchName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            ArchiveFormats.unregisterAll();
        }
    }
    
    public void buildProject() {
        try {
            if (StringUtils.isNotBlank(service) || StringUtils.isNotBlank(frontEndFramework) || StringUtils.isNotBlank(backEndFramework)) {

                if (StringUtils.isNotBlank(frontEndFramework) && StringUtils.isNotBlank(backEndFramework) && (frameworkList.containsKey(backEndFramework + "-" + frontEndFramework) || frameworkList.containsKey(frontEndFramework + "-" + backEndFramework))) {
                    String branchName = frameworkList.containsKey(backEndFramework + "-" + frontEndFramework) ? frameworkList.get(backEndFramework + "-" + frontEndFramework) : frameworkList.get(frontEndFramework + "-" + backEndFramework);
                    branchName = StringUtils.isNotBlank(frontEndAppName) ? frontEndAppName : branchName;
                    createArchieve(git, branchName);
                } else {
                    if (StringUtils.isNotBlank(frontEndFramework)) {
                        if (frameworkList.containsKey(frontEndFramework)) {
                            String branchName = frameworkList.get(frontEndFramework);
                            branchName = StringUtils.isNotBlank(frontEndAppName) ? frontEndAppName : branchName;
                            createArchieve(git, branchName);
                        } else {
                            String branchName = StringUtils.isNotBlank(frontEndAppName) ? frontEndAppName : frontEndFramework;
                            String cloneDirectoryPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + File.separator + "generatedProject";
                            File cloneFile = new File(cloneDirectoryPath);
                            if (!cloneFile.exists()) {
                                FileUtils.forceMkdir(cloneFile);
                            }
                            downLoadFile(cloneFile, branchName);
                        }
                    }
                    if (StringUtils.isNotBlank(backEndFramework)) {
                        if (frameworkList.containsKey(backEndFramework)) {
                            String branchName = frameworkList.get(backEndFramework);
                            branchName = StringUtils.isNotBlank(backEndAppName) ? backEndAppName : branchName;
                            createArchieve(git, branchName);
                        } else {
                            String branchName = StringUtils.isNotBlank(frontEndAppName) ? frontEndAppName : backEndFramework;
                        }
                    }
                }
                if (StringUtils.isNotBlank(service)) {
                    if (frameworkList.containsKey(service)) {
                        String branchName = frameworkList.get(service);
                        branchName = StringUtils.isNotBlank(serviceAppName) ? serviceAppName : branchName;
                        createArchieve(git, branchName);
                    } else {
                        String branchName = StringUtils.isNotBlank(frontEndAppName) ? frontEndAppName : service;
                    }
                }

            } else {
                System.err.println("Please select model");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void downLoadFile(File file, String branchName) {
        try {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            externalContext.setResponseContentType("application/zip");
            externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + branchName + ".zip\"");
            externalContext.setResponseHeader("Content-Length", String.valueOf(file.length()));

            Files.copy(file.toPath(), externalContext.getResponseOutputStream());
            FacesContext.getCurrentInstance().responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getFrameworkList() {
        return frameworkList;
    }

    public void setFrameworkList(Map<String, String> frameworkList) {
        this.frameworkList = frameworkList;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getFrontEndFramework() {
        return frontEndFramework;
    }

    public void setFrontEndFramework(String frontEndFramework) {
        this.frontEndFramework = frontEndFramework;
    }

    public String getBackEndFramework() {
        return backEndFramework;
    }

    public void setBackEndFramework(String backEndFramework) {
        this.backEndFramework = backEndFramework;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getFrontEndAppName() {
        return frontEndAppName;
    }

    public void setFrontEndAppName(String frontEndAppName) {
        this.frontEndAppName = frontEndAppName;
    }

    public String getBackEndAppName() {
        return backEndAppName;
    }

    public void setBackEndAppName(String backEndAppName) {
        this.backEndAppName = backEndAppName;
    }

    public String getServiceAppName() {
        return serviceAppName;
    }

    public void setServiceAppName(String serviceAppName) {
        this.serviceAppName = serviceAppName;
    }

    public Map<String, String> getFrontEndFrameworks() {
        return frontEndFrameworks;
    }

    public void setFrontEndFrameworks(Map<String, String> frontEndFrameworks) {
        this.frontEndFrameworks = frontEndFrameworks;
    }

    public Map<String, String> getBackEndFrameworks() {
        return backEndFrameworks;
    }

    public void setBackEndFrameworks(Map<String, String> backEndFrameworks) {
        this.backEndFrameworks = backEndFrameworks;
    }

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> services) {
        this.services = services;
    }

}
