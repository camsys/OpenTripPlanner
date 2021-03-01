package org.opentripplanner.index.graphql.datafetchers;

import graphql.relay.Relay;
import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.Locale;

import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLAlertCauseType;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLAlertEffectType;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLAlertSeverityLevelType;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

public class GraphQLAlertImpl implements GraphQLDataFetchers.GraphQLAlert {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
	    return environment -> {
	    	AlertPatch e = environment.getSource();
	    	return new Relay.ResolvedGlobalId("AlertPatch", e.getId());
	    };
	}

	@Override
	public DataFetcher<Integer> alertHash() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.hashCode();
		 };
	}

	@Override
	public DataFetcher<String> feed() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.getFeedId();
		 };
	}

	@Override
	public DataFetcher<Object> agency() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.getAgency();
		 };
	}

	@Override
	public DataFetcher<Object> route() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return getGraphIndex(environment).routeForId.get(e.getRoute());
		 };
	}

	@Override
	public DataFetcher<Object> stop() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return getGraphIndex(environment).stopForId.get(e.getRoute());
		 };
	}

	@Override
	public DataFetcher<String> alertHeaderText() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.getAlert().alertHeaderText != null ? e.getAlert().alertHeaderText.toString(Locale.ENGLISH) : null;
		 };
	}

	@Override
	public DataFetcher<String> alertHeaderTextTranslations() {
		return environment -> "";
	}

	@Override
	public DataFetcher<String> alertDescriptionText() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.getAlert().alertDescriptionText != null ? e.getAlert().alertDescriptionText.toString(Locale.ENGLISH) : null;
		 };
	}

	@Override
	public DataFetcher<String> alertDescriptionTextTranslations() {
		return environment -> "";
	}

	@Override
	public DataFetcher<String> alertUrl() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.getAlert().alertUrl != null ? e.getAlert().alertUrl.toString() : null;
		 };
	}

	@Override
	public DataFetcher<String> alertUrlTranslations() {
		return environment -> "";
	}

	@Override
	public DataFetcher<Object> alertEffect() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.isRoutingConsequence() == true ? GraphQLAlertEffectType.MODIFIED_SERVICE : GraphQLAlertEffectType.UNKNOWN_EFFECT;
		 };
	}

	@Override
	public DataFetcher<Object> alertCause() {
		 return environment -> GraphQLAlertCauseType.UNKNOWN_CAUSE;
	}

	@Override
	public DataFetcher<Object> alertSeverityLevel() {
		 return environment -> GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
	}

	@Override
	public DataFetcher<Object> effectiveStartDate() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.getAlert().effectiveStartDate != null ? e.getAlert().effectiveStartDate.getTime() : null;
		 };
	}

	@Override
	public DataFetcher<Object> effectiveEndDate() {
		 return environment -> {
			AlertPatch e = environment.getSource();
		    return e.getAlert().effectiveEndDate != null ? e.getAlert().effectiveEndDate.getTime() : null;
		 };
	}
	
	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
