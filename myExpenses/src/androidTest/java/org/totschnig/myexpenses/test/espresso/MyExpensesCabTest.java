package org.totschnig.myexpenses.test.espresso;

import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.RemoteException;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.Utils;

import java.util.Currency;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.totschnig.myexpenses.test.util.Matchers.withListSize;


@RunWith(AndroidJUnit4.class)
public final class MyExpensesCabTest extends MyExpensesTestBase {

  @Rule
  public ActivityTestRule<MyExpenses> mActivityRule =
      new ActivityTestRule<>(MyExpenses.class);
  private Account account;
  private AdapterIdlingResource adapterIdlingResource;

  @Before
  public void fixture() {
    account = Account.getInstanceFromDb(0);
    Transaction op0 = Transaction.getNewInstance(account.getId());
    op0.setAmount(new Money(Currency.getInstance("USD"),-1200L));
    op0.save();
    int times = 5;
    for (int i = 0; i < times; i++) {
      op0.saveAsNew();
    }
    adapterIdlingResource = new AdapterIdlingResource(getList().getAdapter(), MyExpensesCabTest.class.getSimpleName());
    Espresso.registerIdlingResources(adapterIdlingResource);
    onView(isRoot()).check(matches(anything()));
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    account.reset(null, Account.EXPORT_HANDLE_DELETED_DO_NOTHING, null);
    if (adapterIdlingResource != null) {
      Espresso.unregisterIdlingResources(adapterIdlingResource);
    }
  }

  @Test
  public void cloneCommandIncreasesListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    performContextMenuClick(R.string.menu_clone_transaction, R.id.CLONE_TRANSACTION_COMMAND);
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize + 1)));
  }

  @Test
  public void editCommandKeepsListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    performContextMenuClick(R.string.menu_edit, R.id.EDIT_COMMAND);
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize)));
  }

  @Test
  public void createTemplateCommandCreatesTemplate() {
    String templateTitle = "Espresso Template Test";
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    performContextMenuClick(R.string.menu_create_template_from_transaction, R.id.CREATE_TEMPLATE_COMMAND);
    onView(withText(containsString(mActivityRule.getActivity().getString(R.string.dialog_title_template_title))))
        .check(matches(isDisplayed()));
    onView(withId(R.id.EditTextDialogInput))
        .perform(typeText(templateTitle), closeSoftKeyboard(), pressKey(KeyEvent.KEYCODE_ENTER));

    //((EditText) mSolo.getView(EditText.class, 0)).onEditorAction(EditorInfo.IME_ACTION_DONE);
    onView(withId(R.id.MANAGE_PLANS_COMMAND)).perform(click());
    onView(withText(is(templateTitle))).check(matches(isDisplayed()));
  }

  @Test
  public void deleteCommandDecreasesListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    performContextMenuClick(R.string.menu_delete, R.id.DELETE_COMMAND);
    onView(withText(R.string.menu_delete)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize - 1)));
  }

  @Test
  public void deleteCommandWithVoidOption() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
            .inAdapterView(getWrappedList())
            .atPosition(1) // position 0 is header
            .perform(longClick());
    performContextMenuClick(R.string.menu_delete, R.id.DELETE_COMMAND);
    onView(withId(R.id.checkbox)).perform(click());
    onView(withText(R.string.menu_delete)).perform(click());
    onData(is(instanceOf(Cursor.class))).inAdapterView(getWrappedList()).atPosition(1)
            .check(matches(hasDescendant(both(withId(R.id.voidMarker)).and(isDisplayed()))));
    onView(getWrappedList()).check(matches(withListSize(origListSize)));
  }

  @Test
  public void deleteCommandCancelKeepsListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    performContextMenuClick(R.string.menu_delete, R.id.DELETE_COMMAND);
    onView(withText(android.R.string.cancel)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize)));
  }

  @Test
  public void splitCommandcreatesSplitTransaction() {
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    performContextMenuClick(R.string.menu_split_transaction, R.id.SPLIT_TRANSACTION_COMMAND);
    if (!ContribFeature.SPLIT_TRANSACTION.hasAccess()) {
      onView(withText(R.string.dialog_title_contrib_feature)).check(matches(isDisplayed()));
      onView(withText(R.string.dialog_contrib_no)).perform(scrollTo()).perform(click());
    }
    onView(withText(R.string.split_transaction)).check(matches(isDisplayed()));
    //CursoMatchers class does not allow to distinguish between null and 0 in database
/*    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_CATID, DatabaseConstants.SPLIT_CATID))
        .inAdapterView(allOf(
            isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.list)),
            isDisplayed())).check(matches(isDisplayed()));*/
  }

  private StickyListHeadersListView getList() {
      TransactionList currentFragment = mActivityRule.getActivity().getCurrentFragment();
      return (StickyListHeadersListView) currentFragment.getView().findViewById(R.id.list);
  }

  /**
   * @param legacyString String used on Gingerbread where context actions are rendered in a context menu
   * @param cabId id of menu item rendered in CAB on Honeycomb and higher
   */
  private void performContextMenuClick(int legacyString, int cabId) {
    onView(Utils.hasApiLevel(Build.VERSION_CODES.HONEYCOMB) ? withId(cabId) : withText(legacyString))
        .perform(click());
  }


  private Matcher<View> getWrappedList() {
    return allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.list)),
        isDisplayed());
  }

  private class AdapterIdlingResource implements IdlingResource {
    private String name;
    private ResourceCallback callback;
    private Adapter adapter;

    public AdapterIdlingResource(Adapter adapter, String name) {
      this.adapter = adapter;
      adapter.registerDataSetObserver(new DataSetObserver() {
        @Override
        public void onChanged() {
          if (isIdleNow() && callback != null) {
            callback.onTransitionToIdle();
          }
        }
      });
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isIdleNow() {
      boolean idle = adapter.getCount() > 0;
      return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
      callback = resourceCallback;
    }
  }
}
