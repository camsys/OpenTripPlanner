package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import org.opentripplanner.model.transfer.ConstrainedTransfer;

public class TransferMappingResult{
    public Collection<ConstrainedTransfer> constrainedTransfers;
    public Collection<StaySeatedNotAllowed> staySeatedNotAllowed;

    public TransferMappingResult(Collection<ConstrainedTransfer> constrainedTransfers,
                                 Collection<StaySeatedNotAllowed> staySeatedNotAllowed) {
        this.constrainedTransfers = constrainedTransfers;
        this.staySeatedNotAllowed = staySeatedNotAllowed;
    }


}
