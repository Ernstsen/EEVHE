package dk.mmj.eevhe.entities;

public class BBPackage<T> {
    private T content;

    public BBPackage(T content) {
        this.content = content;
    }

    public BBPackage(){}

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
}
