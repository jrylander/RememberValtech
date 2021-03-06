package cc.rylander.android.remember;

/**
 * Created by Johan Rylander (johan@rylander.cc)
 * on jrylander
 */
public interface QuizRepositoryCallback<T extends QuizRepository> {
    void calledWhenDone(T repository);
}
