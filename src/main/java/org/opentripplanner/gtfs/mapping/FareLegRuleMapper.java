package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.graph_builder.DataImportIssueStore;

public final class FareLegRuleMapper {

  private final FareProductMapper fareProductMapper;
  private final DataImportIssueStore issueStore;

  public FareLegRuleMapper(FareProductMapper fareProductMapper, DataImportIssueStore issueStore) {
    this.fareProductMapper = fareProductMapper;
    this.issueStore = issueStore;
  }

  public Collection<FareLegRule> map(
    Collection<org.onebusaway.gtfs.model.FareLegRule> allFareLegRules
  ) {
    return allFareLegRules
      .stream()
      .map(r -> {
        FareProduct productForRule = fareProductMapper.map(r.getFareProduct());
        if (productForRule != null) {
          return new FareLegRule(
            productForRule.id().getFeedId(),
            r.getNetworkId(),
            r.getFromAreaId(),
            r.getToAreaId(),
            productForRule
          );
        } else {
          issueStore.add(new DataImportIssue() {
                             @Override
                             public String getMessage() {
                                 return "UnknownFareProductId. Fare leg rule refers to unknown fare product" + r.getId() + " " + r.getFareProduct().getId();
                             }
                         }

          );
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }
}
