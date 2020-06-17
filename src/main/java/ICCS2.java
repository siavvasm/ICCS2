/**
 * 
 */


import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import com.google.common.collect.Lists;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
//@SpringBootApplication
//@Configuration
//@EnableAutoConfiguration
//@EnableConfigurationProperties
public class ICCS2 {

	private static final String GIT_OWNER = "apache";
	private static final String GIT_REPO = "commons-io";

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//		ApplicationContext ctx = SpringApplication.run(ICCS1.class, args);

		GitService gitService = new GitServiceImpl();
		Repository repo = gitService.cloneIfNotExists("tmp/" + GIT_REPO, "https://github.com/" + GIT_OWNER +"/"+ GIT_REPO);
		
		GitServiceBean gitServiceBean = new GitServiceBean();
		gitServiceBean.setGit(Git.open(new File("tmp/" + GIT_REPO)));
		gitServiceBean.setDirectory(new File("tmp/" + GIT_REPO));

		List<RevCommit> revCommits = null;
		try {
			revCommits = Lists.newArrayList(gitServiceBean.getGit().log().call());

			Git git = gitServiceBean.getGit();
			Project gitProject;
			
			Project project = null;
			if (Objects.nonNull(project)) {
				gitProject = project;
			}
			else {
				Project createdProject = new Project(GIT_REPO, GIT_OWNER+":"+GIT_REPO);
				gitProject = createdProject;
			}
			
			FilterRevCommits filterRevCommits = new FilterRevCommitsMavenProject();

			Events lastAnalyzedEvent = null;
			Map<String, Commit> alreadyPersistedCommitsMap = new HashMap<>();

			List<RevCommit> revCommitsToBePersisted = revCommits.stream().filter(revCommit -> Objects.isNull(alreadyPersistedCommitsMap.get(revCommit.getName()))).collect(Collectors.toList());

			//Dijkstra Longest Path
			DijkstraLongestPath dlp = new DijkstraLongestPath(revCommits);
			List<RevCommit> candidateRevCommitsForAnalysis = dlp.getDijkstraLongestPathAsRevCommitsList();

			filterRevCommits.setGitServiceBean(gitServiceBean).setGit(git).setRevCommitsAndRevCommitsMap(candidateRevCommitsForAnalysis);
			candidateRevCommitsForAnalysis = filterRevCommits.getFilteredRevCommits();

			candidateRevCommitsForAnalysis = candidateRevCommitsForAnalysis.stream().filter(Objects::nonNull).sorted(Comparator.comparingInt(RevCommit::getCommitTime)).collect(Collectors.toList());
			//candidateRevCommitsForAnalysis = filterOutRevCommitsWithTheSameCommitTime(candidateRevCommitsForAnalysis);
			
			List<RevCommit> revCommitsNew = new ArrayList<>();
			for (int i = 0; i < candidateRevCommitsForAnalysis.size() - 1; i++)
				if (candidateRevCommitsForAnalysis.get(i).getCommitTime() < candidateRevCommitsForAnalysis.get(i + 1).getCommitTime())
					revCommitsNew.add(candidateRevCommitsForAnalysis.get(i));
			candidateRevCommitsForAnalysis = revCommitsNew;
			

			List<RevCommit> revCommitsForAnalysis;
			if (Objects.nonNull(lastAnalyzedEvent))
				for (int i = 0; i < candidateRevCommitsForAnalysis.size(); i++)
					if (Objects.equals(candidateRevCommitsForAnalysis.get(i).getName(), lastAnalyzedEvent.getName()))
						revCommitsForAnalysis = candidateRevCommitsForAnalysis.subList(i, candidateRevCommitsForAnalysis.size());
			revCommitsForAnalysis = candidateRevCommitsForAnalysis;

			//Checks if the last analyzed commit is parent one of the new commits
			if ((revCommitsForAnalysis.size() == candidateRevCommitsForAnalysis.size()) && Objects.nonNull(lastAnalyzedEvent))
				revCommits = new ArrayList<>();
			else
				revCommits = revCommitsForAnalysis;

		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		System.out.println(revCommits.size());
	}

}
