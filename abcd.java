type Query {
    allDevices: [Device]
    searchDevices(apiRequest: ApiRequest): [Device]
    uniqueModels: [String]
    uniqueNetworks: [String]
    dailyTrends(trendsRequest: TrendsRequest): [Trends]
    uniqueUCGSources: [String]
    uniqueUCGSourcesByProject(projectId: ID): [String]
    uniqueVendors: [String]
    uniqueProjects: [Project]
}

type Project {
    id: ID
    name: String
}
@QueryMapping(value = "uniqueUCGSourcesByProject")
public List<String> getUniqueUCGSourcesByProject(@Argument Long projectId) {
    try {
        return uCSPService.getUniqueUCGSourcesByProject(projectId);
    } catch (DataAccessException e) {
        throw new GraphQLException("Database error occurred while fetching UCG Sources: " + e.getMessage());
    } catch (RuntimeException e) {
        throw new GraphQLException("Error: " + e.getMessage());
    } catch (Exception e) {
        throw new GraphQLException("An unexpected error occurred: " + e.getMessage());
    }
}
@Controller
public class UCSPController {

    @Autowired
    private UCSPService uCSPService;

    @QueryMapping(value = "uniqueUCGSourcesByProject")
    public List<String> getUniqueUCGSourcesByProject(@Argument Long projectId) {
        try {
            return uCSPService.getUniqueUCGSourcesByProject(projectId);
        } catch (RuntimeException e) {
            throw new GraphQLException(e.getMessage());
        }
    }
}
@Service
public class UCSPService {

    @Autowired
    private UCSPRepository uCSPRepository;

    // Fetch unique UCG Sources by Project ID
    public List<String> getUniqueUCGSourcesByProject(Long projectId) {
        List<String> ucgSources = uCSPRepository.findDistinctUCGSourcesByProject(projectId);
        if (ucgSources == null || ucgSources.isEmpty()) {
            throw new RuntimeException("No UCG Sources found for the given project ID");
        }
        return ucgSources;
    }
}
@Query(nativeQuery = true, value = "SELECT DISTINCT u.ucgsource FROM ucsp_ucgsources u WHERE u.project_id = :projectId")
List<String> findDistinctUCGSourcesByProject(@Param("projectId") Long projectId);
