package org.opentripplanner.index.graphql.datafetchers;

import graphql.relay.Relay;
import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.util.TranslatedString;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;

public class GraphQLAlertImpl implements GraphQLDataFetchers.GraphQLAlert {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Integer> alertHash() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> feed() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> agency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> route() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> stop() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Iterable<Object>> patterns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> alertHeaderText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> alertHeaderTextTranslations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> alertDescriptionText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> alertDescriptionTextTranslations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> alertUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> alertUrlTranslations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> alertEffect() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> alertCause() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> alertSeverityLevel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> effectiveStartDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> effectiveEndDate() {
		// TODO Auto-generated method stub
		return null;
	}
}
