package domainhealth.rest;



import com.sun.jersey.api.json.JSONConfiguration;
import domainhealth.core.env.AppLog;
import domainhealth.core.env.AppProperties;
import domainhealth.core.jmx.DomainRuntimeServiceMBeanConnection;
import domainhealth.core.statistics.MonitorProperties;
import domainhealth.core.statistics.StatisticsStorage;
import domainhealth.frontend.data.DateAmountDataSet;
import domainhealth.frontend.data.Domain;
import domainhealth.frontend.data.Statistics;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.*;

/**
 * Created by chiovcr on 02/12/2014.
 */
@Path("/")
public class StorageService {

    DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-yyyy-HH-mm");


    @Context
    private ServletContext application;

    StatisticsStorage statisticsStorage;

    @PostConstruct
    public void initialize() {
        try {
            statisticsStorage = new StatisticsStorage((String) application.getAttribute(AppProperties.PropKey.STATS_OUTPUT_PATH_PROP.toString()));
        } catch (Exception sqle) {
            sqle.printStackTrace();
        }
    }


    @GET
    @Path("test")
    @Produces({MediaType.APPLICATION_JSON})
    public Statistics getStatss(@QueryParam("startTime") String startTime,
                                  @QueryParam("endTime") String endTime){
        //JSONConfiguration.mapped().rootUnwrapping(false).build();
        DateTime start = fmt.parseDateTime(startTime);
        DateTime end = fmt.parseDateTime(endTime);
        Interval interval = new Interval(start, end);
        JSONConfiguration.mapped().rootUnwrapping(true).build();
        return new Statistics(interval);
    }

    //http://localhost:7001/domainhealth/rest/stats/core?scope=ALL&startTime=ss&endTime=ss
    //http://localhost:7001/domainhealth/rest/stats/core/xdd?startTime=01-09-2014-00-00&endTime=17-11-2015-0-00
    @GET
    @Path("stats/{resourceType}/{resource}")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String,Map<String,DateAmountDataSet>>  getStats(@QueryParam("scope") List<String> scope,
                           @QueryParam("startTime") String startTime,
                           @QueryParam("endTime") String endTime,
                           @PathParam("resourceType") String resourceType,
                           @PathParam("resource") String resource) {

        try {
            System.out.println("Core");
            Map<String,Map<String,DateAmountDataSet>> result = new HashMap<String, Map<String, DateAmountDataSet>>();
            //String temp = resource;
            DateTime start = fmt.parseDateTime(startTime);
            DateTime end = fmt.parseDateTime(endTime);
            Interval interval = new Interval(start, end);
            DomainRuntimeServiceMBeanConnection conn = null;
            System.out.println(scope);
            // //ex: StorageUtil.getPropertyData(statisticsStorage,"core",null,"HeapUsedCurrent",new Date(),1,"AdminServer");
            if (scope==null || scope.size()==0){
                conn = new DomainRuntimeServiceMBeanConnection();
                Set<String> servers = statisticsStorage.getAllPossibleServerNames(conn);
                for (String server:servers){
                    System.out.println("Nu scope");
                 //   OpenSocketsCurrentCount	HeapSizeCurrent	HeapFreeCurrent	HeapUsedCurrent
                    Set<String> coreProps = new HashSet<String>();
                    coreProps.add("OpenSocketsCurrentCount");
                    coreProps.add("HeapUsedCurrent");
                    result.put(server, statisticsStorage.getPropertyData(resourceType, null, coreProps, interval, server));
                }

            }
            return result;
        } catch (Exception e ){
            e.printStackTrace();
        }

        return null;
    }

    //http://localhost:7001/domainhealth/rest/resources?startTime=01-09-2014-00-00&endTime=17-09-2016-0-00
    @GET
    @Path("resources")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String,Set<String>> getStats(
                                @QueryParam("startTime") String startTime,
                                @QueryParam("endTime") String endTime
                               ) {

        Map<String,Set<String>> resourcesMap = new HashMap<String, Set<String>>();
        try {
            DateTime start = fmt.parseDateTime(startTime);
            DateTime end = fmt.parseDateTime(endTime);
            Interval interval = new Interval(start, end);

                resourcesMap.put(MonitorProperties.CORE_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.CORE_RESOURCE_TYPE));
                resourcesMap.put(MonitorProperties.DATASOURCE_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.DATASOURCE_RESOURCE_TYPE));
                resourcesMap.put(MonitorProperties.DESTINATION_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.DESTINATION_RESOURCE_TYPE));
                resourcesMap.put(MonitorProperties.SAF_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.SAF_RESOURCE_TYPE));
                resourcesMap.put(MonitorProperties.EJB_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.EJB_RESOURCE_TYPE));
                resourcesMap.put(MonitorProperties.WORKMGR_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.WORKMGR_RESOURCE_TYPE));
                resourcesMap.put(MonitorProperties.WEBAPP_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.WEBAPP_RESOURCE_TYPE));
                resourcesMap.put(MonitorProperties.SVRCHNL_RESOURCE_TYPE,statisticsStorage.getResourceNamesFromPropsListForInterval(interval, MonitorProperties.SVRCHNL_RESOURCE_TYPE));

        } catch (IOException e) {
            //TODO handle exception
            e.printStackTrace();
        }
        return resourcesMap;
    }

    @GET
    @Path("/domain")
    @Produces({MediaType.APPLICATION_JSON})
    public Set<String> getDomain() {
        Domain domain;
        DomainRuntimeServiceMBeanConnection conn = null;
        try {
            conn = new DomainRuntimeServiceMBeanConnection();
            Set<String> servers = statisticsStorage.getAllPossibleServerNames(conn);
            return servers;
        } catch (Exception e) {
            AppLog.getLogger().error(e.toString());
            e.printStackTrace();
            AppLog.getLogger().error("Statistics Retriever Background Service - unable to retrieve domain structure for domain's servers for this iteration");
        } finally {
            if (conn != null) {
                conn.close();
            }
        }


        return null;
    }


}
