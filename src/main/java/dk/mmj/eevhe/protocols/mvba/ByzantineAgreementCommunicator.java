package dk.mmj.eevhe.protocols.mvba;

public interface ByzantineAgreementCommunicator<T> {

    /**
     * Initiates the Byzantine Agreement Protocol.
     * <br>
     * When the agreement protocol terminates, the notifyItem will be updated
     *
     * @param message the message for the protocol to agree on
     * @return notifyItem which will be updated with result, once finished
     */
    BANotifyItem<T> agree(T message);

    class BANotifyItem<T> extends NotifyItem {
        private T message;
        private Boolean agreement;

        BANotifyItem() {
        }

        /**
         * Values:
         * <ul>
         *     <li>True: Value was agree upon </li>
         *     <li>False: Value was not agreed upon</li>
         *     <li>Null: The protocol was not able to reach a conclusion </li>
         * </ul>
         *
         * @return result from protocol termination
         */
        public Boolean getAgreement() {
            return agreement;
        }

        public void setMessage(T message) {
            this.message = message;
        }

        void setAgreement(Boolean agreement) {
            this.agreement = agreement;
        }

        public T getMessage() {
            return message;
        }
    }
}
